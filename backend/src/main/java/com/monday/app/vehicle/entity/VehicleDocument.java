package com.monday.app.vehicle.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class VehicleDocument {
    private UUID id;
    private UUID vehicleId;
    private String docType;
    private String title;
    private String identifier;
    private String issuer;
    private String validUntil;
    private LocalDate issueDate;
    private LocalDate validFrom;
    private LocalDate expiryDate;
    private String verificationSource;
    private Boolean isVerified;
    private String status;
    private String coveredComponents;
    private String notes;
    private String metadata;
    private Instant createdAt;
    private Instant updatedAt;

    public VehicleDocument() {}

    public VehicleDocument(UUID id, UUID vehicleId, String docType, String title,
                           String identifier, String issuer, String validUntil,
                           LocalDate issueDate, LocalDate validFrom, LocalDate expiryDate,
                           String verificationSource, Boolean isVerified, String status,
                           String coveredComponents, String notes, String metadata,
                           Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.docType = docType;
        this.title = title;
        this.identifier = identifier;
        this.issuer = issuer;
        this.validUntil = validUntil;
        this.issueDate = issueDate;
        this.validFrom = validFrom;
        this.expiryDate = expiryDate;
        this.verificationSource = verificationSource;
        this.isVerified = isVerified;
        this.status = status;
        this.coveredComponents = coveredComponents;
        this.notes = notes;
        this.metadata = metadata;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getValidUntil() { return validUntil; }
    public void setValidUntil(String validUntil) { this.validUntil = validUntil; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate validFrom) { this.validFrom = validFrom; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public String getVerificationSource() { return verificationSource; }
    public void setVerificationSource(String verificationSource) { this.verificationSource = verificationSource; }
    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean verified) { isVerified = verified; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCoveredComponents() { return coveredComponents; }
    public void setCoveredComponents(String coveredComponents) { this.coveredComponents = coveredComponents; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
