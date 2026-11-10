package com.monday.app.agenda.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class BetaDailyBrief {
    private UUID id;
    private LocalDate briefDate;
    private String generationMode;
    private String status;
    private String snapshotJson;
    private String briefingJson;
    private Instant createdAt;
    private Instant updatedAt;

    public BetaDailyBrief() {}

    public BetaDailyBrief(UUID id, LocalDate briefDate, String generationMode, String status,
                          String snapshotJson, String briefingJson, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.briefDate = briefDate;
        this.generationMode = generationMode;
        this.status = status;
        this.snapshotJson = snapshotJson;
        this.briefingJson = briefingJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LocalDate getBriefDate() { return briefDate; }
    public void setBriefDate(LocalDate briefDate) { this.briefDate = briefDate; }

    public String getGenerationMode() { return generationMode; }
    public void setGenerationMode(String generationMode) { this.generationMode = generationMode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }

    public String getBriefingJson() { return briefingJson; }
    public void setBriefingJson(String briefingJson) { this.briefingJson = briefingJson; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

