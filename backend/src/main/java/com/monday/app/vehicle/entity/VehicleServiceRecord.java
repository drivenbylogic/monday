package com.monday.app.vehicle.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class VehicleServiceRecord {
    private UUID id;
    private UUID vehicleId;
    private LocalDate serviceDate;
    private Integer kmAtService;
    private String serviceType;
    private String description;
    private BigDecimal cost;
    private BigDecimal partsCost;
    private BigDecimal laborCost;
    private String performedBy;
    private String notes;
    private Boolean verified;
    private Boolean isUpcoming;
    private String linkedTransactionId;
    private String specificationsJson;
    private Instant createdAt;
    private Instant updatedAt;

    public VehicleServiceRecord() {}

    public VehicleServiceRecord(UUID id, UUID vehicleId, LocalDate serviceDate, Integer kmAtService,
                                String serviceType, String description, BigDecimal cost,
                                String performedBy, String notes, Instant createdAt, Instant updatedAt) {
        this.id = id; this.vehicleId = vehicleId; this.serviceDate = serviceDate;
        this.kmAtService = kmAtService; this.serviceType = serviceType;
        this.description = description; this.cost = cost; this.performedBy = performedBy;
        this.notes = notes; this.createdAt = createdAt; this.updatedAt = updatedAt;
        this.partsCost = BigDecimal.ZERO;
        this.laborCost = BigDecimal.ZERO;
        this.verified = false;
        this.isUpcoming = false;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }
    public LocalDate getServiceDate() { return serviceDate; }
    public void setServiceDate(LocalDate serviceDate) { this.serviceDate = serviceDate; }
    public Integer getKmAtService() { return kmAtService; }
    public void setKmAtService(Integer kmAtService) { this.kmAtService = kmAtService; }
    public String getServiceType() { return serviceType; }
    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public BigDecimal getPartsCost() { return partsCost; }
    public void setPartsCost(BigDecimal partsCost) { this.partsCost = partsCost; }
    public BigDecimal getLaborCost() { return laborCost; }
    public void setLaborCost(BigDecimal laborCost) { this.laborCost = laborCost; }
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Boolean getVerified() { return verified; }
    public void setVerified(Boolean verified) { this.verified = verified; }
    public Boolean getIsUpcoming() { return isUpcoming; }
    public void setIsUpcoming(Boolean upcoming) { isUpcoming = upcoming; }
    public String getLinkedTransactionId() { return linkedTransactionId; }
    public void setLinkedTransactionId(String linkedTransactionId) { this.linkedTransactionId = linkedTransactionId; }
    public String getSpecificationsJson() { return specificationsJson; }
    public void setSpecificationsJson(String specificationsJson) { this.specificationsJson = specificationsJson; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
