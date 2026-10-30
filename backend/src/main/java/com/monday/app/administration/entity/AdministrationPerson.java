package com.monday.app.administration.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class AdministrationPerson {

    private UUID id;
    private String displayName;
    private String relationshipLabel;
    private LocalDate birthday;
    private String contactInformation;
    private String notes;
    private Instant archivedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public AdministrationPerson() {}

    public AdministrationPerson(UUID id, String displayName, String relationshipLabel,
                                LocalDate birthday, String contactInformation, String notes,
                                Instant archivedAt, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.displayName = displayName;
        this.relationshipLabel = relationshipLabel;
        this.birthday = birthday;
        this.contactInformation = contactInformation;
        this.notes = notes;
        this.archivedAt = archivedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getRelationshipLabel() { return relationshipLabel; }
    public void setRelationshipLabel(String relationshipLabel) { this.relationshipLabel = relationshipLabel; }

    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate birthday) { this.birthday = birthday; }

    public String getContactInformation() { return contactInformation; }
    public void setContactInformation(String contactInformation) { this.contactInformation = contactInformation; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
