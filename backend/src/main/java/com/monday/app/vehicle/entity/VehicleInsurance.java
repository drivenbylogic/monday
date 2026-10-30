package com.monday.app.vehicle.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class VehicleInsurance {
    private UUID id;
    private UUID vehicleId;
    private UUID documentId;
    private String policyName;
    private String underwriter;
    private String policyNumber;
    private String coverageScope;
    private BigDecimal insuredDeclaredValue;
    private BigDecimal premiumDue;
    private LocalDate dueDate;
    private String status;
    private String packageDocumentUrl;
    private Boolean isRenewed;
    private Instant createdAt;
    private Instant updatedAt;

    public VehicleInsurance() {}

    public VehicleInsurance(UUID id, UUID vehicleId, UUID documentId, String policyName,
                            String underwriter, String policyNumber, String coverageScope,
                            BigDecimal insuredDeclaredValue, BigDecimal premiumDue, LocalDate dueDate,
                            String status, String packageDocumentUrl, Boolean isRenewed,
                            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.documentId = documentId;
        this.policyName = policyName;
        this.underwriter = underwriter;
        this.policyNumber = policyNumber;
        this.coverageScope = coverageScope;
        this.insuredDeclaredValue = insuredDeclaredValue;
        this.premiumDue = premiumDue;
        this.dueDate = dueDate;
        this.status = status;
        this.packageDocumentUrl = packageDocumentUrl;
        this.isRenewed = isRenewed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public String getPolicyName() { return policyName; }
    public void setPolicyName(String policyName) { this.policyName = policyName; }
    public String getUnderwriter() { return underwriter; }
    public void setUnderwriter(String underwriter) { this.underwriter = underwriter; }
    public String getPolicyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }
    public String getCoverageScope() { return coverageScope; }
    public void setCoverageScope(String coverageScope) { this.coverageScope = coverageScope; }
    public BigDecimal getInsuredDeclaredValue() { return insuredDeclaredValue; }
    public void setInsuredDeclaredValue(BigDecimal insuredDeclaredValue) { this.insuredDeclaredValue = insuredDeclaredValue; }
    public BigDecimal getPremiumDue() { return premiumDue; }
    public void setPremiumDue(BigDecimal premiumDue) { this.premiumDue = premiumDue; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPackageDocumentUrl() { return packageDocumentUrl; }
    public void setPackageDocumentUrl(String packageDocumentUrl) { this.packageDocumentUrl = packageDocumentUrl; }
    public Boolean getIsRenewed() { return isRenewed; }
    public void setIsRenewed(Boolean renewed) { isRenewed = renewed; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
