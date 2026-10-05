package com.monday.app.reminder.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Reminder domain entity. Linked to exactly one of task_id or event_id.
 */
public final class Reminder {

    private UUID id;
    private UUID taskId;
    private UUID eventId;
    private Instant remindAt;
    private String title;
    private String description;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public Reminder() {}

    public Reminder(UUID id, UUID taskId, UUID eventId, Instant remindAt,
                    String title, String description, String status,
                    Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.taskId = taskId;
        this.eventId = eventId;
        this.remindAt = remindAt;
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTaskId() { return taskId; }
    public void setTaskId(UUID taskId) { this.taskId = taskId; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public Instant getRemindAt() { return remindAt; }
    public void setRemindAt(Instant remindAt) { this.remindAt = remindAt; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
