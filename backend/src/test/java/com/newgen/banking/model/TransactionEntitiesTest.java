package com.newgen.banking.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionEntitiesTest {

    @Test
    @DisplayName("BankTransaction constructors, getters, setters, and toString work properly")
    void testBankTransactionEntity() {
        Account source = new Account();
        source.setId(1L);
        Account target = new Account();
        target.setId(2L);

        BankTransaction tx = new BankTransaction(
                "TXN-TEST-001",
                source,
                target,
                "TRANSFER",
                new BigDecimal("250.00"),
                "USD",
                "SUCCESS",
                "Test transfer"
        );
        tx.setId(10L);

        assertThat(tx.getId()).isEqualTo(10L);
        assertThat(tx.getTransactionReference()).isEqualTo("TXN-TEST-001");
        assertThat(tx.getSourceAccount()).isEqualTo(source);
        assertThat(tx.getTargetAccount()).isEqualTo(target);
        assertThat(tx.getTransactionType()).isEqualTo("TRANSFER");
        assertThat(tx.getAmount()).isEqualByComparingTo("250.00");
        assertThat(tx.getCurrency()).isEqualTo("USD");
        assertThat(tx.getStatus()).isEqualTo("SUCCESS");
        assertThat(tx.getDescription()).isEqualTo("Test transfer");
        assertThat(tx.getCreatedAt()).isNotNull();
        assertThat(tx.toString()).contains("TXN-TEST-001", "TRANSFER", "SUCCESS");
    }

    @Test
    @DisplayName("TransactionAudit constructors, getters, setters, and toString work properly")
    void testTransactionAuditEntity() {
        BankTransaction tx = new BankTransaction();
        tx.setId(1L);
        Account account = new Account();
        account.setId(1L);

        TransactionAudit audit = new TransactionAudit(
                tx,
                account,
                "CREDIT",
                new BigDecimal("100.00"),
                new BigDecimal("900.00"),
                new BigDecimal("1000.00")
        );
        audit.setId(5L);

        assertThat(audit.getId()).isEqualTo(5L);
        assertThat(audit.getTransaction()).isEqualTo(tx);
        assertThat(audit.getAccount()).isEqualTo(account);
        assertThat(audit.getActionType()).isEqualTo("CREDIT");
        assertThat(audit.getAmount()).isEqualByComparingTo("100.00");
        assertThat(audit.getBalanceBefore()).isEqualByComparingTo("900.00");
        assertThat(audit.getBalanceAfter()).isEqualByComparingTo("1000.00");
        assertThat(audit.getCreatedAt()).isNotNull();
        assertThat(audit.toString()).contains("CREDIT", "1000.00");
    }

    @Test
    @DisplayName("FailedTransaction constructors, getters, setters, and toString work properly")
    void testFailedTransactionEntity() {
        Account source = new Account();
        source.setId(3L);
        Account target = new Account();
        target.setId(4L);
        OffsetDateTime now = OffsetDateTime.now();

        FailedTransaction failed = new FailedTransaction(
                2L,
                "TXN-FAIL-001",
                source,
                target,
                "TRANSFER",
                new BigDecimal("5000.00"),
                "INSUFFICIENT_FUNDS",
                "Insufficient balance",
                now
        );

        assertThat(failed.getId()).isEqualTo(2L);
        assertThat(failed.getTransactionReference()).isEqualTo("TXN-FAIL-001");
        assertThat(failed.getSourceAccount()).isEqualTo(source);
        assertThat(failed.getTargetAccount()).isEqualTo(target);
        assertThat(failed.getTransactionType()).isEqualTo("TRANSFER");
        assertThat(failed.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(failed.getErrorCode()).isEqualTo("INSUFFICIENT_FUNDS");
        assertThat(failed.getErrorMessage()).isEqualTo("Insufficient balance");
        assertThat(failed.getFailedAt()).isEqualTo(now);
        assertThat(failed.toString()).contains("TXN-FAIL-001", "INSUFFICIENT_FUNDS");
    }
}
