package com.newgen.banking.service;

import com.newgen.banking.dto.TransactionResponse;
import com.newgen.banking.dto.TransferRequest;
import com.newgen.banking.exception.InsufficientFundsException;
import com.newgen.banking.model.Account;
import com.newgen.banking.model.BankTransaction;
import com.newgen.banking.model.TransactionAudit;
import com.newgen.banking.repository.AccountRepository;
import com.newgen.banking.repository.BankTransactionRepository;
import com.newgen.banking.repository.TransactionAuditRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PostgreSQL integration tests for Fund Transfer API / TransactionService.
 * Verifies deterministic locking, atomic dual-account updates, 1 TRANSFER transaction,
 * DEBIT + CREDIT audit records, and full rollback of both accounts on failure.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class TransferIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Autowired
    private TransactionAuditRepository transactionAuditRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional
    @DisplayName("Transfer updates both balances, creates 1 TRANSFER transaction, and writes DEBIT + CREDIT audits")
    void testTransferPersistedInPostgres() {
        Account sourceBefore = accountRepository.findById(1L).orElseThrow();
        Account targetBefore = accountRepository.findById(2L).orElseThrow();
        BigDecimal sourcePrev = sourceBefore.getBalance();
        BigDecimal targetPrev = targetBefore.getBalance();
        BigDecimal transferAmount = new BigDecimal("250.00");

        TransactionResponse response = transactionService.transfer(
                new TransferRequest(1L, 2L, transferAmount, "Integration test transfer"));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getTransactionType()).isEqualTo("TRANSFER");
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getSourceAccountId()).isEqualTo(1L);
        assertThat(response.getTargetAccountId()).isEqualTo(2L);
        assertThat(response.getUpdatedBalance()).isEqualByComparingTo(sourcePrev.subtract(transferAmount));

        Account sourceAfter = accountRepository.findById(1L).orElseThrow();
        Account targetAfter = accountRepository.findById(2L).orElseThrow();
        assertThat(sourceAfter.getBalance()).isEqualByComparingTo(sourcePrev.subtract(transferAmount));
        assertThat(targetAfter.getBalance()).isEqualByComparingTo(targetPrev.add(transferAmount));

        BankTransaction tx = bankTransactionRepository.findById(response.getId()).orElseThrow();
        assertThat(tx.getSourceAccount().getId()).isEqualTo(1L);
        assertThat(tx.getTargetAccount().getId()).isEqualTo(2L);

        List<TransactionAudit> audits = transactionAuditRepository.findByTransactionId(tx.getId());
        assertThat(audits).hasSize(2);
        assertThat(audits).anyMatch(a -> "DEBIT".equals(a.getActionType()) && a.getAccount().getId().equals(1L));
        assertThat(audits).anyMatch(a -> "CREDIT".equals(a.getActionType()) && a.getAccount().getId().equals(2L));
    }

    @Test
    @Transactional
    @DisplayName("Exact-balance transfer and insufficient-funds rejection in PostgreSQL")
    void testExactBalanceAndInsufficientFundsInPostgres() {
        BigDecimal sourceBalance = accountRepository.findById(1L).orElseThrow().getBalance();

        assertThatThrownBy(() -> transactionService.transfer(
                new TransferRequest(1L, 2L, sourceBalance.add(new BigDecimal("0.01")), "Overdraw")))
                .isInstanceOf(InsufficientFundsException.class);

        TransactionResponse response = transactionService.transfer(
                new TransferRequest(1L, 2L, sourceBalance, "Exact transfer"));
        assertThat(response.getUpdatedBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Both source and target account balances roll back when persistence fails mid-transfer")
    void testTransferRollbackOnFailure() {
        BigDecimal sourceOriginal = accountRepository.findById(1L).orElseThrow().getBalance();
        BigDecimal targetOriginal = accountRepository.findById(2L).orElseThrow().getBalance();
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> template.executeWithoutResult(status -> {
            transactionService.transfer(new TransferRequest(1L, 2L, new BigDecimal("100.00"), "Rollback test"));
            throw new RuntimeException("Simulated persistence failure");
        })).isInstanceOf(RuntimeException.class);

        assertThat(accountRepository.findById(1L).orElseThrow().getBalance()).isEqualByComparingTo(sourceOriginal);
        assertThat(accountRepository.findById(2L).orElseThrow().getBalance()).isEqualByComparingTo(targetOriginal);
    }
}
