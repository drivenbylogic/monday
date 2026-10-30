package com.monday.app.finance.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class CreditAccount {

    private UUID id;
    private String cardId;
    private String cardName;
    private String lastFour;
    private String network;
    private BigDecimal creditLimit;
    private BigDecimal currentBalance;
    private LocalDate statementClosingDate;
    private LocalDate paymentDueDate;
    private BigDecimal minAmountDue;
    private boolean autoPayEnabled;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public CreditAccount() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCardId() { return cardId; }
    public void setCardId(String cardId) { this.cardId = cardId; }
    public String getCardName() { return cardName; }
    public void setCardName(String cardName) { this.cardName = cardName; }
    public String getLastFour() { return lastFour; }
    public void setLastFour(String lastFour) { this.lastFour = lastFour; }
    public String getNetwork() { return network; }
    public void setNetwork(String network) { this.network = network; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    public LocalDate getBillingDate() {
        return statementClosingDate;
    }
    public void setBillingDate(LocalDate billingDate) {
        this.statementClosingDate = billingDate;
    }

    public LocalDate getDueDate() {
        return paymentDueDate;
    }
    public void setDueDate(LocalDate dueDate) {
        this.paymentDueDate = dueDate;
    }

    public LocalDate getStatementClosingDate() { return statementClosingDate; }
    public void setStatementClosingDate(LocalDate statementClosingDate) { this.statementClosingDate = statementClosingDate; }
    public LocalDate getPaymentDueDate() { return paymentDueDate; }
    public void setPaymentDueDate(LocalDate paymentDueDate) { this.paymentDueDate = paymentDueDate; }
    public BigDecimal getMinAmountDue() { return minAmountDue; }
    public void setMinAmountDue(BigDecimal minAmountDue) { this.minAmountDue = minAmountDue; }
    public boolean isAutoPayEnabled() { return autoPayEnabled; }
    public void setAutoPayEnabled(boolean autoPayEnabled) { this.autoPayEnabled = autoPayEnabled; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
