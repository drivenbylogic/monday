package com.monday.app.brief.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class DailyBrief {
    private UUID id;
    private LocalDate briefDate;
    private String structuredAgendaJson;
    private String aiSummaryMarkdown;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public DailyBrief() {}

    public DailyBrief(UUID id, LocalDate briefDate, String structuredAgendaJson, String aiSummaryMarkdown, String status, Instant createdAt, Instant updatedAt) {
        this.id = id; this.briefDate = briefDate; this.structuredAgendaJson = structuredAgendaJson;
        this.aiSummaryMarkdown = aiSummaryMarkdown; this.status = status;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LocalDate getBriefDate() { return briefDate; }
    public void setBriefDate(LocalDate briefDate) { this.briefDate = briefDate; }
    public String getStructuredAgendaJson() { return structuredAgendaJson; }
    public void setStructuredAgendaJson(String structuredAgendaJson) { this.structuredAgendaJson = structuredAgendaJson; }
    public String getAiSummaryMarkdown() { return aiSummaryMarkdown; }
    public void setAiSummaryMarkdown(String aiSummaryMarkdown) { this.aiSummaryMarkdown = aiSummaryMarkdown; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
