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
    private String performedBy;
    private String notes;
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
    public String getPerformedBy() { return performedBy; }
    public void setPerformedBy(String performedBy) { this.performedBy = performedBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
