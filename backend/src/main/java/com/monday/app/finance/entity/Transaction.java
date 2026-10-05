package com.monday.app.finance.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class Transaction {

    private UUID id;
    private String transactionType;
    private BigDecimal amount;
    private Instant transactionAt;
    private UUID categoryId;
    private String paymentMethod;
    private String description;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public Transaction() {}

    public Transaction(UUID id, String transactionType, BigDecimal amount, Instant transactionAt,
                       UUID categoryId, String paymentMethod, String description, String notes,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionAt = transactionAt;
        this.categoryId = categoryId;
        this.paymentMethod = paymentMethod;
        this.description = description;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Instant getTransactionAt() { return transactionAt; }
    public void setTransactionAt(Instant transactionAt) { this.transactionAt = transactionAt; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
