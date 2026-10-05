package com.monday.app.intelligence.entity;

import java.time.Instant;
import java.util.UUID;

public final class IndustrySource {
    private UUID id;
    private String name;
    private String feedUrl;
    private String sourceType;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public IndustrySource() {}

    public IndustrySource(UUID id, String name, String feedUrl, String sourceType,
                          boolean active, Instant createdAt, Instant updatedAt) {
        this.id = id; this.name = name; this.feedUrl = feedUrl;
        this.sourceType = sourceType; this.active = active;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFeedUrl() { return feedUrl; }
    public void setFeedUrl(String feedUrl) { this.feedUrl = feedUrl; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
