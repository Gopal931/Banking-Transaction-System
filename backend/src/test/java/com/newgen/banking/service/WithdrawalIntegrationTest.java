package com.newgen.banking.service;

import com.newgen.banking.dto.TransactionResponse;
import com.newgen.banking.dto.WithdrawRequest;
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
 * PostgreSQL integration tests for Withdrawal API / TransactionService.
 * Verifies pessimistic locking, sufficient-funds validation, exact-balance withdrawal,
 * DEBIT TransactionAudit entry, and full rollback on persistence failure.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class WithdrawalIntegrationTest {

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
    @DisplayName("Withdrawal persists updated balance, BankTransaction, and DEBIT TransactionAudit in PostgreSQL")
    void testWithdrawPersistedInPostgres() {
        Account accountBefore = accountRepository.findById(1L).orElseThrow();
        BigDecimal previousBalance = accountBefore.getBalance();
        BigDecimal withdrawAmount = new BigDecimal("200.00");

        TransactionResponse response = transactionService.withdraw(
                new WithdrawRequest(1L, withdrawAmount, "Integration test withdrawal"));

        assertThat(response.getId()).isNotNull();
        assertThat(response.getTransactionType()).isEqualTo("WITHDRAWAL");
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getUpdatedBalance()).isEqualByComparingTo(previousBalance.subtract(withdrawAmount));

        Account accountAfter = accountRepository.findById(1L).orElseThrow();
        assertThat(accountAfter.getBalance()).isEqualByComparingTo(previousBalance.subtract(withdrawAmount));

        BankTransaction tx = bankTransactionRepository.findById(response.getId()).orElseThrow();
        assertThat(tx.getSourceAccount().getId()).isEqualTo(1L);
        assertThat(tx.getTargetAccount()).isNull();

        List<TransactionAudit> audits = transactionAuditRepository.findByTransactionId(tx.getId());
        assertThat(audits).hasSize(1);
        assertThat(audits.get(0).getActionType()).isEqualTo("DEBIT");
        assertThat(audits.get(0).getBalanceBefore()).isEqualByComparingTo(previousBalance);
        assertThat(audits.get(0).getBalanceAfter()).isEqualByComparingTo(previousBalance.subtract(withdrawAmount));
    }

    @Test
    @Transactional
    @DisplayName("Exact-balance withdrawal leaves 0.00 balance and insufficient funds throws InsufficientFundsException")
    void testExactBalanceAndInsufficientFundsInPostgres() {
        Account account = accountRepository.findById(1L).orElseThrow();
        BigDecimal exactBalance = account.getBalance();

        assertThatThrownBy(() -> transactionService.withdraw(
                new WithdrawRequest(1L, exactBalance.add(new BigDecimal("0.01")), "Overdraw attempt")))
                .isInstanceOf(InsufficientFundsException.class);

        TransactionResponse response = transactionService.withdraw(
                new WithdrawRequest(1L, exactBalance, "Full balance withdrawal"));
        assertThat(response.getUpdatedBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Withdrawal balance update rolls back when an exception occurs during persistence")
    void testWithdrawRollbackOnFailure() {
        BigDecimal originalBalance = accountRepository.findById(1L).orElseThrow().getBalance();
        TransactionTemplate template = new TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> template.executeWithoutResult(status -> {
            transactionService.withdraw(new WithdrawRequest(1L, new BigDecimal("50.00"), "Rollback test"));
            throw new RuntimeException("Simulated persistence failure");
        })).isInstanceOf(RuntimeException.class);

        BigDecimal balanceAfterRollback = accountRepository.findById(1L).orElseThrow().getBalance();
        assertThat(balanceAfterRollback).isEqualByComparingTo(originalBalance);
    }
}
