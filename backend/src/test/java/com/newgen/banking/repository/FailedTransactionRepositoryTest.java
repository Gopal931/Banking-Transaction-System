package com.newgen.banking.repository;

import com.newgen.banking.model.Account;
import com.newgen.banking.model.FailedTransaction;
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
 * Integration tests for FailedTransactionRepository using the existing PostgreSQL database.
 * Annotated with @Transactional so test-created records are rolled back safely.
 * Requires DB_PASSWORD environment variable to connect to PostgreSQL.
 */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class FailedTransactionRepositoryTest {

    @Autowired
    private FailedTransactionRepository failedTransactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("1. Save and retrieve a failed transaction")
    void testSaveAndRetrieveFailedTransaction() {
        Account source = accountRepository.findById(3L)
                .orElseThrow(() -> new IllegalStateException("Seed account id=3 must exist"));
        Account target = accountRepository.findById(4L)
                .orElseThrow(() -> new IllegalStateException("Seed account id=4 must exist"));

        String uniqueRef = "TXN-FAIL-TST-" + System.currentTimeMillis();
        FailedTransaction failed = new FailedTransaction();
        failed.setTransactionReference(uniqueRef);
        failed.setSourceAccount(source);
        failed.setTargetAccount(target);
        failed.setTransactionType("TRANSFER");
        failed.setAmount(new BigDecimal("75000.00"));
        failed.setErrorCode("LIMIT_EXCEEDED");
        failed.setErrorMessage("JUnit test failure record");
        failed.setFailedAt(OffsetDateTime.now());

        FailedTransaction saved = failedTransactionRepository.save(failed);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0L);

        Optional<FailedTransaction> retrieved = failedTransactionRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTransactionReference()).isEqualTo(uniqueRef);
        assertThat(retrieved.get().getErrorCode()).isEqualTo("LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("2. Find failed transactions by error code and transaction reference")
    void testFindByErrorCodeAndTransactionReference() {
        List<FailedTransaction> byErrorCode = failedTransactionRepository.findByErrorCode("INSUFFICIENT_FUNDS");
        assertThat(byErrorCode).isNotEmpty();
        assertThat(byErrorCode).allMatch(f -> "INSUFFICIENT_FUNDS".equals(f.getErrorCode()));

        List<FailedTransaction> byRef = failedTransactionRepository.findByTransactionReference("TXN-20261124-901");
        assertThat(byRef).isNotEmpty();
        assertThat(byRef.get(0).getErrorCode()).isEqualTo("INSUFFICIENT_FUNDS");
    }
}
