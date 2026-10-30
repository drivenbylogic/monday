package com.monday.app.finance.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class Budget {

    private UUID id;
    private String name;
    private UUID categoryId;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal amount;
    private String periodType = "MONTHLY";
    private Instant createdAt;
    private Instant updatedAt;

    public Budget() {}

    public Budget(UUID id, String name, UUID categoryId, LocalDate periodStart,
                  LocalDate periodEnd, BigDecimal amount, String periodType, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.categoryId = categoryId;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.amount = amount;
        this.periodType = (periodType != null && !periodType.trim().isEmpty()) ? periodType : "MONTHLY";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
