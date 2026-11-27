package com.newgen.banking.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.newgen.banking.model.BankTransaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

 //Response DTO returned after transaction processing.

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResponse {

    private Long id;
    private String transactionReference;
    private Long sourceAccountId;
    private Long targetAccountId;
    private String transactionType;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String description;
    private BigDecimal updatedBalance;
    private OffsetDateTime createdAt;

    public TransactionResponse() {
    }

    public TransactionResponse(Long id, String transactionReference, Long sourceAccountId, Long targetAccountId,
                               String transactionType, BigDecimal amount, String currency, String status,
                               String description, BigDecimal updatedBalance, OffsetDateTime createdAt) {
        this.id = id;
        this.transactionReference = transactionReference;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.transactionType = transactionType;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.description = description;
        this.updatedBalance = updatedBalance;
        this.createdAt = createdAt;
    }

    public static TransactionResponse fromEntity(BankTransaction tx, BigDecimal updatedBalance) {
        return new TransactionResponse(
                tx.getId(),
                tx.getTransactionReference(),
                tx.getSourceAccount() != null ? tx.getSourceAccount().getId() : null,
                tx.getTargetAccount() != null ? tx.getTargetAccount().getId() : null,
                tx.getTransactionType(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getStatus(),
                tx.getDescription(),
                updatedBalance,
                tx.getCreatedAt()
        );
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

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(Long sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public Long getTargetAccountId() {
        return targetAccountId;
    }

    public void setTargetAccountId(Long targetAccountId) {
        this.targetAccountId = targetAccountId;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getUpdatedBalance() {
        return updatedBalance;
    }

    public void setUpdatedBalance(BigDecimal updatedBalance) {
        this.updatedBalance = updatedBalance;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
