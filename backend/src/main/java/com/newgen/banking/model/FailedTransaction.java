package com.newgen.banking.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * FailedTransaction Entity representing the 'failed_transaction' database table.
 * Records rejected transaction attempts and their error details.
 */
@Entity
@Table(name = "failed_transaction")
public class FailedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_reference", nullable = false, length = 64)
    private String transactionReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id")
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id")
    private Account targetAccount;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "error_code", nullable = false, length = 50)
    private String errorCode;

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "failed_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private OffsetDateTime failedAt;

    public FailedTransaction() {
    }

    public FailedTransaction(String transactionReference, Account sourceAccount, Account targetAccount,
                             String transactionType, BigDecimal amount, String errorCode, String errorMessage) {
        this.transactionReference = transactionReference;
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.transactionType = transactionType;
        this.amount = amount;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.failedAt = OffsetDateTime.now();
    }

    public FailedTransaction(Long id, String transactionReference, Account sourceAccount, Account targetAccount,
                             String transactionType, BigDecimal amount, String errorCode,
                             String errorMessage, OffsetDateTime failedAt) {
        this.id = id;
        this.transactionReference = transactionReference;
        this.sourceAccount = sourceAccount;
        this.targetAccount = targetAccount;
        this.transactionType = transactionType;
        this.amount = amount;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.failedAt = failedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(Account sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public Account getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(Account targetAccount) {
        this.targetAccount = targetAccount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public OffsetDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(OffsetDateTime failedAt) {
        this.failedAt = failedAt;
    }

    @Override
    public String toString() {
        return "FailedTransaction{" +
                "id=" + id +
                ", transactionReference='" + transactionReference + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", amount=" + amount +
                ", errorCode='" + errorCode + '\'' +
                ", failedAt=" + failedAt +
                '}';
    }
}
