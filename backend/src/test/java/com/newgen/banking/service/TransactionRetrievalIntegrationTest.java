package com.newgen.banking.service;

import com.newgen.banking.dto.PagedResponse;
import com.newgen.banking.dto.TransactionResponse;
import com.newgen.banking.exception.InvalidRequestException;
import com.newgen.banking.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PostgreSQL integration tests for Transaction Retrieval APIs.
 * Verifies retrieving DEPOSIT, WITHDRAWAL, and TRANSFER transactions by ID and reference,
 * nullable source/target handling, and 404/400 error cases against live banking_db.
 */
@SpringBootTest
@Transactional
@EnabledIfEnvironmentVariable(named = "DB_PASSWORD", matches = ".+")
class TransactionRetrievalIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Test
    @DisplayName("Retrieve DEPOSIT, WITHDRAWAL, and TRANSFER seeded transactions by ID and reference")
    void testRetrieveSeededTransactions() {
        // Seed id=1: DEPOSIT (source null, target=1)
        TransactionResponse deposit = transactionService.getTransactionById(1L);
        assertThat(deposit.getTransactionReference()).isEqualTo("TXN-20261120-001");
        assertThat(deposit.getTransactionType()).isEqualTo("DEPOSIT");
        assertThat(deposit.getSourceAccountId()).isNull();
        assertThat(deposit.getTargetAccountId()).isEqualTo(1L);
        assertThat(deposit.getStatus()).isEqualTo("SUCCESS");

        // Seed id=2: WITHDRAWAL (source=1, target null)
        TransactionResponse withdrawal = transactionService.getTransactionByReference("TXN-20261121-002");
        assertThat(withdrawal.getId()).isEqualTo(2L);
        assertThat(withdrawal.getTransactionType()).isEqualTo("WITHDRAWAL");
        assertThat(withdrawal.getSourceAccountId()).isEqualTo(1L);
        assertThat(withdrawal.getTargetAccountId()).isNull();

        // Seed id=3: TRANSFER (source=1, target=2)
        TransactionResponse transfer = transactionService.getTransactionById(3L);
        assertThat(transfer.getTransactionReference()).isEqualTo("TXN-20261122-003");
        assertThat(transfer.getTransactionType()).isEqualTo("TRANSFER");
        assertThat(transfer.getSourceAccountId()).isEqualTo(1L);
        assertThat(transfer.getTargetAccountId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Missing transaction ID, missing reference, and blank reference throw expected exceptions")
    void testTransactionNotFoundAndBlankReference() {
        assertThatThrownBy(() -> transactionService.getTransactionById(999999L))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> transactionService.getTransactionByReference("TXN-DOES-NOT-EXIST"))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() -> transactionService.getTransactionByReference("  "))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    @DisplayName("Paginated transaction history for account 1 and target account 2 in PostgreSQL")
    void testAccountTransactionHistoryPaginationInPostgres() {
        PagedResponse<TransactionResponse> page0 = transactionService.getAccountTransactions(1L, 0, 2);
        assertThat(page0.getPage()).isEqualTo(0);
        assertThat(page0.getSize()).isEqualTo(2);
        assertThat(page0.getTotalElements()).isGreaterThanOrEqualTo(3);
        assertThat(page0.isHasNext()).isTrue();
        assertThat(page0.isHasPrevious()).isFalse();
        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.getContent().get(0).getCreatedAt())
                .isAfterOrEqualTo(page0.getContent().get(1).getCreatedAt());

        PagedResponse<TransactionResponse> page1 = transactionService.getAccountTransactions(1L, 1, 2);
        assertThat(page1.getPage()).isEqualTo(1);
        assertThat(page1.isHasPrevious()).isTrue();

        // Account 2 is target of transfer id=3
        PagedResponse<TransactionResponse> acc2History = transactionService.getAccountTransactions(2L, 0, 10);
        assertThat(acc2History.getContent())
                .extracting(TransactionResponse::getTransactionReference)
                .contains("TXN-20261122-003");

        assertThatThrownBy(() -> transactionService.getAccountTransactions(999999L, 0, 10))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> transactionService.getAccountTransactions(1L, -1, 10))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> transactionService.getAccountTransactions(1L, 0, 0))
                .isInstanceOf(InvalidRequestException.class);
    }
}
