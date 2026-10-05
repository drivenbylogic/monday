package com.monday.app.project.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Unified task domain entity.
 *
 * <p>Serves all task contexts: personal, work, career, event preparation.
 * Recurring templates generate child occurrences via {@code parentTaskId}.</p>
 */
public final class Task {

    private UUID id;
    private String title;
    private String description;
    private UUID projectId;
    private UUID milestoneId;
    private UUID eventId;
    private UUID parentTaskId;
    private String status;
    private String priority;
    private Instant dueAt;
    private Instant startedAt;
    private Instant completedAt;
    private Integer estimatedMinutes;
    private boolean recurring;
    private String recurrencePattern;
    private Instant createdAt;
    private Instant updatedAt;

    public Task() {}

    public Task(UUID id, String title, String description,
                UUID projectId, UUID milestoneId, UUID eventId, UUID parentTaskId,
                String status, String priority,
                Instant dueAt, Instant startedAt, Instant completedAt,
                Integer estimatedMinutes, boolean recurring, String recurrencePattern,
                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.projectId = projectId;
        this.milestoneId = milestoneId;
        this.eventId = eventId;
        this.parentTaskId = parentTaskId;
        this.status = status;
        this.priority = priority;
        this.dueAt = dueAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.estimatedMinutes = estimatedMinutes;
        this.recurring = recurring;
        this.recurrencePattern = recurrencePattern;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public UUID getMilestoneId() { return milestoneId; }
    public void setMilestoneId(UUID milestoneId) { this.milestoneId = milestoneId; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public UUID getParentTaskId() { return parentTaskId; }
    public void setParentTaskId(UUID parentTaskId) { this.parentTaskId = parentTaskId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }

    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }

    public String getRecurrencePattern() { return recurrencePattern; }
    public void setRecurrencePattern(String recurrencePattern) { this.recurrencePattern = recurrencePattern; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
