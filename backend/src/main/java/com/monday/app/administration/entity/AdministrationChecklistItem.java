package com.monday.app.administration.entity;

import java.time.Instant;
import java.util.UUID;

public class AdministrationChecklistItem {

    private UUID id;
    private UUID checklistId;
    private String title;
    private String description;
    private int position;
    private boolean completed;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public AdministrationChecklistItem() {}

    public AdministrationChecklistItem(UUID id, UUID checklistId, String title,
                                       String description, int position, boolean completed,
                                       Instant completedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.checklistId = checklistId;
        this.title = title;
        this.description = description;
        this.position = position;
        this.completed = completed;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getChecklistId() { return checklistId; }
    public void setChecklistId(UUID checklistId) { this.checklistId = checklistId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
