package com.newgen.banking.repository;

import com.newgen.banking.model.Account;
import com.newgen.banking.model.BankTransaction;
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
 * Integration tests for BankTransactionRepository using the existing PostgreSQL database.
 * Annotated with @Transactional so test-created records are rolled back safely.
 * Requires DB_PASSWORD environment variable to connect to PostgreSQL.
 */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class BankTransactionRepositoryTest {

    @Autowired
    private BankTransactionRepository bankTransactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("1. Save and retrieve a bank transaction")
    void testSaveAndRetrieveBankTransaction() {
        Account targetAccount = accountRepository.findById(1L)
                .orElseThrow(() -> new IllegalStateException("Seed account id=1 must exist"));

        String uniqueRef = "TXN-TST-" + System.currentTimeMillis();
        BankTransaction tx = new BankTransaction();
        tx.setTransactionReference(uniqueRef);
        tx.setSourceAccount(null);
        tx.setTargetAccount(targetAccount);
        tx.setTransactionType("DEPOSIT");
        tx.setAmount(new BigDecimal("250.00"));
        tx.setCurrency("USD");
        tx.setStatus("SUCCESS");
        tx.setDescription("JUnit test deposit");
        tx.setCreatedAt(OffsetDateTime.now());

        BankTransaction saved = bankTransactionRepository.save(tx);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0L);

        Optional<BankTransaction> retrieved = bankTransactionRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getTransactionReference()).isEqualTo(uniqueRef);
        assertThat(retrieved.get().getAmount()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("2. Find transaction by transaction reference")
    void testFindByTransactionReference() {
        Optional<BankTransaction> found = bankTransactionRepository.findByTransactionReference("TXN-20261120-001");

        assertThat(found).isPresent();
        assertThat(found.get().getTransactionType()).isEqualTo("DEPOSIT");
        assertThat(found.get().getStatus()).isEqualTo("SUCCESS");

        Optional<BankTransaction> notFound = bankTransactionRepository.findByTransactionReference("TXN-DOES-NOT-EXIST");
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("3. Find transactions by status and transaction type")
    void testFindByStatusAndType() {
        List<BankTransaction> successful = bankTransactionRepository.findByStatus("SUCCESS");
        assertThat(successful).isNotEmpty();
        assertThat(successful.size()).isGreaterThanOrEqualTo(4);
        assertThat(successful).allMatch(t -> "SUCCESS".equals(t.getStatus()));

        List<BankTransaction> transfers = bankTransactionRepository.findByTransactionType("TRANSFER");
        assertThat(transfers).isNotEmpty();
        assertThat(transfers).allMatch(t -> "TRANSFER".equals(t.getTransactionType()));
    }
}
