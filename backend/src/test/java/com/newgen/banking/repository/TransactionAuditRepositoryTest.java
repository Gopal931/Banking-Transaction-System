package com.newgen.banking.repository;

import com.newgen.banking.model.Account;
import com.newgen.banking.model.BankTransaction;
import com.newgen.banking.model.TransactionAudit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for TransactionAuditRepository using the existing PostgreSQL database.
 * Annotated with @Transactional so test-created records are rolled back safely.
 * Requires DB_PASSWORD environment variable to connect to PostgreSQL.
 */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class TransactionAuditRepositoryTest {

    @Autowired
    private TransactionAuditRepository transactionAuditRepository;

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("1. Save and retrieve an audit record")
    void testSaveAndRetrieveAuditRecord() {
        BankTransaction existingTx = bankTransactionRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Seed transaction id=1 must exist"));
        Account existingAccount = accountRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Seed account id=1 must exist"));

        TransactionAudit audit = new TransactionAudit();
        audit.setTransaction(existingTx);
        audit.setAccount(existingAccount);
        audit.setActionType("CREDIT");
        audit.setAmount(new BigDecimal("100.00"));
        audit.setBalanceBefore(new BigDecimal("12450.25"));
        audit.setBalanceAfter(new BigDecimal("12550.25"));
        audit.setCreatedAt(OffsetDateTime.now());

        TransactionAudit saved = transactionAuditRepository.save(audit);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0L);

        Optional<TransactionAudit> retrieved = transactionAuditRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getActionType()).isEqualTo("CREDIT");
        assertThat(retrieved.get().getBalanceAfter()).isEqualByComparingTo("12550.25");
    }

    @Test
    @DisplayName("2. Find audit records by account ID and transaction ID")
    void testFindByAccountIdAndTransactionId() {
        List<TransactionAudit> byAccount = transactionAuditRepository.findByAccountId(1L);
        assertThat(byAccount).isNotEmpty();
        assertThat(byAccount.size()).isGreaterThanOrEqualTo(3);

        List<TransactionAudit> byTransaction = transactionAuditRepository.findByTransactionId(3L);
        assertThat(byTransaction).isNotEmpty();
        assertThat(byTransaction.size()).isGreaterThanOrEqualTo(2);
    }
}
