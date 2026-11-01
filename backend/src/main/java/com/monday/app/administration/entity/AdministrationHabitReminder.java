package com.monday.app.administration.entity;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

public class AdministrationHabitReminder {

    private UUID id;
    private UUID habitId;
    private LocalTime reminderTime;
    private boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;

    public AdministrationHabitReminder() {}

    public AdministrationHabitReminder(UUID id, UUID habitId, LocalTime reminderTime,
                                       boolean enabled, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.habitId = habitId;
        this.reminderTime = reminderTime;
        this.enabled = enabled;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getHabitId() { return habitId; }
    public void setHabitId(UUID habitId) { this.habitId = habitId; }

    public LocalTime getReminderTime() { return reminderTime; }
    public void setReminderTime(LocalTime reminderTime) { this.reminderTime = reminderTime; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

