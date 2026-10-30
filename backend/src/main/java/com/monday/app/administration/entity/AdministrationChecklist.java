package com.monday.app.administration.entity;

import java.time.Instant;
import java.util.UUID;

public class AdministrationChecklist {

    private UUID id;
    private String title;
    private String description;
    private String administrationRecordType; // OBLIGATION, PERSON, IMPORTANT_DATE, GADGET, MAINTENANCE, APPOINTMENT
    private UUID administrationRecordId;
    private String status; // ACTIVE, COMPLETED, ARCHIVED
    private Instant createdAt;
    private Instant updatedAt;

    public AdministrationChecklist() {}

    public AdministrationChecklist(UUID id, String title, String description,
                                   String administrationRecordType, UUID administrationRecordId,
                                   String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.administrationRecordType = administrationRecordType;
        this.administrationRecordId = administrationRecordId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAdministrationRecordType() { return administrationRecordType; }
    public void setAdministrationRecordType(String administrationRecordType) { this.administrationRecordType = administrationRecordType; }

    public UUID getAdministrationRecordId() { return administrationRecordId; }
    public void setAdministrationRecordId(UUID administrationRecordId) { this.administrationRecordId = administrationRecordId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
