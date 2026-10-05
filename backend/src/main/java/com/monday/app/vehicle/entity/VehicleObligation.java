package com.monday.app.vehicle.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class VehicleObligation {
    private UUID id;
    private UUID vehicleId;
    private String obligationType;
    private LocalDate dueDate;
    private BigDecimal amount;
    private String status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public VehicleObligation() {}
    public VehicleObligation(UUID id, UUID vehicleId, String obligationType, LocalDate dueDate,
                             BigDecimal amount, String status, String notes,
                             Instant createdAt, Instant updatedAt) {
        this.id = id; this.vehicleId = vehicleId; this.obligationType = obligationType;
        this.dueDate = dueDate; this.amount = amount; this.status = status;
        this.notes = notes; this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }
    public String getObligationType() { return obligationType; }
    public void setObligationType(String obligationType) { this.obligationType = obligationType; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
