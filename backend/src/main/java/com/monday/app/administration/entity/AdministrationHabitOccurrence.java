package com.monday.app.administration.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class AdministrationHabitOccurrence {

    private UUID id;
    private UUID habitId;
    private UUID scheduleVersionId;
    private LocalDate scheduledDate;
    private BigDecimal targetValueSnapshot;
    private BigDecimal minimumTargetSnapshot;
    private String targetUnitSnapshot;
    private String status; // PENDING, COMPLETED, PARTIAL, MISSED, SKIPPED
    private BigDecimal actualValue;
    private Instant completedAt;
    private String notes;
    private String skipReason;
    private Instant createdAt;
    private Instant updatedAt;

    public AdministrationHabitOccurrence() {}

    public AdministrationHabitOccurrence(UUID id, UUID habitId, UUID scheduleVersionId,
                                         LocalDate scheduledDate, BigDecimal targetValueSnapshot,
                                         BigDecimal minimumTargetSnapshot, String targetUnitSnapshot,
                                         String status, BigDecimal actualValue, Instant completedAt,
                                         String notes, String skipReason, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.habitId = habitId;
        this.scheduleVersionId = scheduleVersionId;
        this.scheduledDate = scheduledDate;
        this.targetValueSnapshot = targetValueSnapshot;
        this.minimumTargetSnapshot = minimumTargetSnapshot;
        this.targetUnitSnapshot = targetUnitSnapshot;
        this.status = status;
        this.actualValue = actualValue;
        this.completedAt = completedAt;
        this.notes = notes;
        this.skipReason = skipReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getHabitId() { return habitId; }
    public void setHabitId(UUID habitId) { this.habitId = habitId; }

    public UUID getScheduleVersionId() { return scheduleVersionId; }
    public void setScheduleVersionId(UUID scheduleVersionId) { this.scheduleVersionId = scheduleVersionId; }

    public LocalDate getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(LocalDate scheduledDate) { this.scheduledDate = scheduledDate; }

    public BigDecimal getTargetValueSnapshot() { return targetValueSnapshot; }
    public void setTargetValueSnapshot(BigDecimal targetValueSnapshot) { this.targetValueSnapshot = targetValueSnapshot; }

    public BigDecimal getMinimumTargetSnapshot() { return minimumTargetSnapshot; }
    public void setMinimumTargetSnapshot(BigDecimal minimumTargetSnapshot) { this.minimumTargetSnapshot = minimumTargetSnapshot; }

    public String getTargetUnitSnapshot() { return targetUnitSnapshot; }
    public void setTargetUnitSnapshot(String targetUnitSnapshot) { this.targetUnitSnapshot = targetUnitSnapshot; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getActualValue() { return actualValue; }
    public void setActualValue(BigDecimal actualValue) { this.actualValue = actualValue; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getSkipReason() { return skipReason; }
    public void setSkipReason(String skipReason) { this.skipReason = skipReason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

