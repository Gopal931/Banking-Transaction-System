package com.newgen.banking.service;

import com.newgen.banking.dto.DepositRequest;
import com.newgen.banking.dto.TransactionResponse;
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
 * PostgreSQL integration tests for Deposit API / TransactionService.
 * Verifies atomic balance updates, BankTransaction creation, CREDIT TransactionAudit entry,
 * and full rollback on persistence failure.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class DepositIntegrationTest {

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
    @DisplayName("Deposit persists updated balance, BankTransaction, and CREDIT TransactionAudit in PostgreSQL")
    void testDepositPersistedInPostgres() {
        Account accountBefore = accountRepository.findById(1L).orElseThrow();
        BigDecimal previousBalance = accountBefore.getBalance();
        BigDecimal depositAmount = new BigDecimal("500.00");

        TransactionResponse response = transactionService.deposit(
                new DepositRequest(1L, depositAmount, "Integration test deposit"));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getTransactionType()).isEqualTo("DEPOSIT");
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getUpdatedBalance()).isEqualByComparingTo(previousBalance.add(depositAmount));

        Account accountAfter = accountRepository.findById(1L).orElseThrow();
        assertThat(accountAfter.getBalance()).isEqualByComparingTo(previousBalance.add(depositAmount));

        BankTransaction tx = bankTransactionRepository.findById(response.getId()).orElseThrow();
        assertThat(tx.getTargetAccount().getId()).isEqualTo(1L);
        assertThat(tx.getSourceAccount()).isNull();

        List<TransactionAudit> audits = transactionAuditRepository.findByTransactionId(tx.getId());
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).getActionType()).isEqualTo("CREDIT");
        assertThat(audits.get(0).getBalanceBefore()).isEqualByComparingTo(previousBalance);
        assertThat(audits.get(0).getBalanceAfter()).isEqualByComparingTo(previousBalance.add(depositAmount));
    }

    @Test
    @DisplayName("Balance update rolls back when an exception occurs during transaction/audit persistence")
    void testDepositRollbackOnFailure() {
        BigDecimal originalBalance = accountRepository.findById(1L).orElseThrow().getBalance();
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> template.executeWithoutResult(status -> {
            transactionService.deposit(new DepositRequest(1L, new BigDecimal("100.00"), "Rollback test"));
            throw new RuntimeException("Simulated persistence failure");
        })).isInstanceOf(RuntimeException.class);

        BigDecimal balanceAfterRollback = accountRepository.findById(1L).orElseThrow().getBalance();
        assertThat(balanceAfterRollback).isEqualByComparingTo(originalBalance);
    }
}
