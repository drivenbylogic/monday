package com.monday.app.administration.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class AdministrationHabit {

    private UUID id;
    private UUID userId;
    private UUID groupId;
    private String name;
    private String description;
    private String category;
    private String frequencyType; // DAILY, SELECTED_DAYS, WEEKLY_TARGET, INTERVAL_DAYS, MONTHLY, CUSTOM
    private BigDecimal targetValue;
    private BigDecimal minimumTargetValue;
    private String targetUnit;
    private String priority; // LOW, MEDIUM, HIGH
    private LocalDate startDate;
    private LocalDate endDate;
    private String status; // ACTIVE, PAUSED, ARCHIVED
    private int displayOrder;
    private Integer estimatedDuration; // in minutes
    private Instant createdAt;
    private Instant updatedAt;

    public AdministrationHabit() {}

    public AdministrationHabit(UUID id, UUID userId, UUID groupId, String name, String description,
                               String category, String frequencyType, BigDecimal targetValue,
                               BigDecimal minimumTargetValue, String targetUnit, String priority,
                               LocalDate startDate, LocalDate endDate, String status,
                               int displayOrder, Integer estimatedDuration, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.groupId = groupId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.frequencyType = frequencyType;
        this.targetValue = targetValue;
        this.minimumTargetValue = minimumTargetValue;
        this.targetUnit = targetUnit;
        this.priority = priority;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.displayOrder = displayOrder;
        this.estimatedDuration = estimatedDuration;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getFrequencyType() { return frequencyType; }
    public void setFrequencyType(String frequencyType) { this.frequencyType = frequencyType; }

    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }

    public BigDecimal getMinimumTargetValue() { return minimumTargetValue; }
    public void setMinimumTargetValue(BigDecimal minimumTargetValue) { this.minimumTargetValue = minimumTargetValue; }

    public String getTargetUnit() { return targetUnit; }
    public void setTargetUnit(String targetUnit) { this.targetUnit = targetUnit; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public Integer getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(Integer estimatedDuration) { this.estimatedDuration = estimatedDuration; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

