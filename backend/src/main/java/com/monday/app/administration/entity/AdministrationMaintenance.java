package com.monday.app.administration.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class AdministrationMaintenance {

    private UUID id;
    private UUID gadgetId;
    private String title;
    private String description;
    private String maintenanceType; // SERVICING, CLEANING, INSPECTION, BATTERY_CHECK, FILTER_REPLACEMENT, REPAIR, OTHER
    private LocalDate dueDate;
    private String recurrenceRule; // EVERY_MONTH, EVERY_3_MONTHS, EVERY_6_MONTHS, EVERY_YEAR, etc.
    private LocalDate lastPerformedAt;
    private BigDecimal cost;
    private String status; // UPCOMING, DUE, COMPLETED, CANCELLED
    private String serviceProvider;
    private String serviceNotes;
    private Instant createdAt;
    private Instant updatedAt;

    public AdministrationMaintenance() {}

    public AdministrationMaintenance(UUID id, UUID gadgetId, String title, String description,
                                     String maintenanceType, LocalDate dueDate, String recurrenceRule,
                                     LocalDate lastPerformedAt, BigDecimal cost, String status,
                                     String serviceProvider, String serviceNotes,
                                     Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.gadgetId = gadgetId;
        this.title = title;
        this.description = description;
        this.maintenanceType = maintenanceType;
        this.dueDate = dueDate;
        this.recurrenceRule = recurrenceRule;
        this.lastPerformedAt = lastPerformedAt;
        this.cost = cost;
        this.status = status;
        this.serviceProvider = serviceProvider;
        this.serviceNotes = serviceNotes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getGadgetId() { return gadgetId; }
    public void setGadgetId(UUID gadgetId) { this.gadgetId = gadgetId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMaintenanceType() { return maintenanceType; }
    public void setMaintenanceType(String maintenanceType) { this.maintenanceType = maintenanceType; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getRecurrenceRule() { return recurrenceRule; }
    public void setRecurrenceRule(String recurrenceRule) { this.recurrenceRule = recurrenceRule; }

    public LocalDate getLastPerformedAt() { return lastPerformedAt; }
    public void setLastPerformedAt(LocalDate lastPerformedAt) { this.lastPerformedAt = lastPerformedAt; }

    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getServiceProvider() { return serviceProvider; }
    public void setServiceProvider(String serviceProvider) { this.serviceProvider = serviceProvider; }

    public String getServiceNotes() { return serviceNotes; }
    public void setServiceNotes(String serviceNotes) { this.serviceNotes = serviceNotes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
