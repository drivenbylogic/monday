package com.monday.app.administration.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class AdministrationObligation {

    private UUID id;
    private String name;
    private String description;
    private String providerName;
    private String category;
    private String obligationType; // BILL, SUBSCRIPTION, MEMBERSHIP, OTHER
    private BigDecimal expectedAmount;
    private String currency; // Default USD
    private String billingFrequency; // WEEKLY, MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY, CUSTOM
    private String recurrenceRule;
    private LocalDate startDate;
    private LocalDate nextDueDate;
    private LocalDate endDate;
    private LocalDate trialEndDate;
    private LocalDate cancellationDeadline;
    private boolean autoRenew;
    private String status; // ACTIVE, PENDING, PAID, CANCELLED, EXPIRED, REVIEWED
    private Instant archivedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public AdministrationObligation() {}

    public AdministrationObligation(UUID id, String name, String description, String providerName,
                                  String category, String obligationType, BigDecimal expectedAmount,
                                  String currency, String billingFrequency, String recurrenceRule,
                                  LocalDate startDate, LocalDate nextDueDate, LocalDate endDate,
                                  LocalDate trialEndDate, LocalDate cancellationDeadline,
                                  boolean autoRenew, String status, Instant archivedAt,
                                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.providerName = providerName;
        this.category = category;
        this.obligationType = obligationType;
        this.expectedAmount = expectedAmount;
        this.currency = currency;
        this.billingFrequency = billingFrequency;
        this.recurrenceRule = recurrenceRule;
        this.startDate = startDate;
        this.nextDueDate = nextDueDate;
        this.endDate = endDate;
        this.trialEndDate = trialEndDate;
        this.cancellationDeadline = cancellationDeadline;
        this.autoRenew = autoRenew;
        this.status = status;
        this.archivedAt = archivedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getObligationType() { return obligationType; }
    public void setObligationType(String obligationType) { this.obligationType = obligationType; }

    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getBillingFrequency() { return billingFrequency; }
    public void setBillingFrequency(String billingFrequency) { this.billingFrequency = billingFrequency; }

    public String getRecurrenceRule() { return recurrenceRule; }
    public void setRecurrenceRule(String recurrenceRule) { this.recurrenceRule = recurrenceRule; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getNextDueDate() { return nextDueDate; }
    public void setNextDueDate(LocalDate nextDueDate) { this.nextDueDate = nextDueDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDate getTrialEndDate() { return trialEndDate; }
    public void setTrialEndDate(LocalDate trialEndDate) { this.trialEndDate = trialEndDate; }

    public LocalDate getCancellationDeadline() { return cancellationDeadline; }
    public void setCancellationDeadline(LocalDate cancellationDeadline) { this.cancellationDeadline = cancellationDeadline; }

    public boolean isAutoRenew() { return autoRenew; }
    public void setAutoRenew(boolean autoRenew) { this.autoRenew = autoRenew; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
