package com.monday.app.project.entity;

import java.time.Instant;
import java.util.UUID;

/**
 * Project domain entity.
 */
public final class Project {

    private UUID id;
    private String name;
    private String description;
    private String category;
    private String status;
    private String priority;
    private String tags;
    private Instant startAt;
    private Instant dueAt;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public Project() {}

    public Project(UUID id, String name, String description, String category, String status, String priority, String tags,
                   Instant startAt, Instant dueAt, Instant completedAt,
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.status = status;
        this.priority = priority;
        this.tags = tags;
        this.startAt = startAt;
        this.dueAt = dueAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }

    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
