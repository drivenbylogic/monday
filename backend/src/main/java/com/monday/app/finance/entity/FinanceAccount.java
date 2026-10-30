package com.monday.app.finance.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class FinanceAccount {

    private UUID id;
    private String accountName;
    private String institution;
    private String accountType;
    private BigDecimal balance;
    private String currency;
    private Instant lastReconciled;
    private Instant createdAt;
    private Instant updatedAt;

    public FinanceAccount() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getLastReconciled() { return lastReconciled; }
    public void setLastReconciled(Instant lastReconciled) { this.lastReconciled = lastReconciled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
