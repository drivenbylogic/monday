package com.monday.app.administration.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class AdministrationHabitPausePeriod {

    private UUID id;
    private UUID habitId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private Instant createdAt;

    public AdministrationHabitPausePeriod() {}

    public AdministrationHabitPausePeriod(UUID id, UUID habitId, LocalDate startDate,
                                          LocalDate endDate, String reason, Instant createdAt) {
        this.id = id;
        this.habitId = habitId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getHabitId() { return habitId; }
    public void setHabitId(UUID habitId) { this.habitId = habitId; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

