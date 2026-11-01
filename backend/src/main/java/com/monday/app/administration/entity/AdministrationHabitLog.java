package com.monday.app.administration.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class AdministrationHabitLog {

    private UUID id;
    private UUID habitId;
    private LocalDate entryDate;
    private BigDecimal actualValue;
    private String status; // COMPLETED, PARTIAL
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public AdministrationHabitLog() {}

    public AdministrationHabitLog(UUID id, UUID habitId, LocalDate entryDate,
                                  BigDecimal actualValue, String status, String notes,
                                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.habitId = habitId;
        this.entryDate = entryDate;
        this.actualValue = actualValue;
        this.status = status;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getHabitId() { return habitId; }
    public void setHabitId(UUID habitId) { this.habitId = habitId; }

    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }

    public BigDecimal getActualValue() { return actualValue; }
    public void setActualValue(BigDecimal actualValue) { this.actualValue = actualValue; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

