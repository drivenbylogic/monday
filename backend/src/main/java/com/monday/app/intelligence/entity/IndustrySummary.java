package com.monday.app.intelligence.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class IndustrySummary {
    private UUID id;
    private LocalDate summaryDate;
    private String title;
    private String contentMarkdown;
    private String sourcesJson;
    private Instant createdAt;
    private Instant updatedAt;

    public IndustrySummary() {}

    public IndustrySummary(UUID id, LocalDate summaryDate, String title,
                           String contentMarkdown, String sourcesJson,
                           Instant createdAt, Instant updatedAt) {
        this.id = id; this.summaryDate = summaryDate; this.title = title;
        this.contentMarkdown = contentMarkdown; this.sourcesJson = sourcesJson;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LocalDate getSummaryDate() { return summaryDate; }
    public void setSummaryDate(LocalDate summaryDate) { this.summaryDate = summaryDate; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContentMarkdown() { return contentMarkdown; }
    public void setContentMarkdown(String contentMarkdown) { this.contentMarkdown = contentMarkdown; }
    public String getSourcesJson() { return sourcesJson; }
    public void setSourcesJson(String sourcesJson) { this.sourcesJson = sourcesJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
