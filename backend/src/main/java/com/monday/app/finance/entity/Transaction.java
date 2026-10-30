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
    private String linkedModule;
    private String linkedEntityId;
    private String linkedEntityType;
    private UUID accountId;
    private UUID creditCardId;
    private String categoryName;
    private Instant createdAt;
    private Instant updatedAt;

    public Transaction() {}

    public Transaction(UUID id, String transactionType, BigDecimal amount, Instant transactionAt,
                       UUID categoryId, String paymentMethod, String description, String notes,
                       Instant createdAt, Instant updatedAt) {
        this(id, transactionType, amount, transactionAt, categoryId, paymentMethod, description, notes,
             null, null, null, null, createdAt, updatedAt);
    }

    public Transaction(UUID id, String transactionType, BigDecimal amount, Instant transactionAt,
                       UUID categoryId, String paymentMethod, String description, String notes,
                       String linkedModule, String linkedEntityId, String linkedEntityType,
                       Instant createdAt, Instant updatedAt) {
        this(id, transactionType, amount, transactionAt, categoryId, paymentMethod, description, notes,
             linkedModule, linkedEntityId, linkedEntityType, null, createdAt, updatedAt);
    }

    public Transaction(UUID id, String transactionType, BigDecimal amount, Instant transactionAt,
                       UUID categoryId, String paymentMethod, String description, String notes,
                       String linkedModule, String linkedEntityId, String linkedEntityType,
                       UUID accountId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.transactionType = transactionType;
        this.amount = amount;
        this.transactionAt = transactionAt;
        this.categoryId = categoryId;
        this.paymentMethod = paymentMethod;
        this.description = description;
        this.notes = notes;
        this.linkedModule = linkedModule;
        this.linkedEntityId = linkedEntityId;
        this.linkedEntityType = linkedEntityType;
        this.accountId = accountId;
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
    public String getLinkedModule() { return linkedModule; }
    public void setLinkedModule(String linkedModule) { this.linkedModule = linkedModule; }
    public String getLinkedEntityId() { return linkedEntityId; }
    public void setLinkedEntityId(String linkedEntityId) { this.linkedEntityId = linkedEntityId; }
    public String getLinkedEntityType() { return linkedEntityType; }
    public void setLinkedEntityType(String linkedEntityType) { this.linkedEntityType = linkedEntityType; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getCreditCardId() { return creditCardId; }
    public void setCreditCardId(UUID creditCardId) { this.creditCardId = creditCardId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
