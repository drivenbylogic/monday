package com.monday.app.vehicle.entity;

import java.time.Instant;
import java.util.UUID;

public final class VehicleTask {
    private UUID id;
    private UUID vehicleId;
    private String title;
    private String metaText;
    private Boolean completed;
    private String priority;
    private String linkedModule;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public VehicleTask() {}

    public VehicleTask(UUID id, UUID vehicleId, String title, String metaText,
                       Boolean completed, String priority, String linkedModule,
                       Instant completedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.title = title;
        this.metaText = metaText;
        this.completed = Boolean.TRUE.equals(completed);
        this.priority = priority != null ? priority : "MEDIUM";
        this.linkedModule = linkedModule;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMetaText() { return metaText; }
    public void setMetaText(String metaText) { this.metaText = metaText; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getLinkedModule() { return linkedModule; }
    public void setLinkedModule(String linkedModule) { this.linkedModule = linkedModule; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
