package com.monday.app.vehicle.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class VehicleMaintenanceSchedule {
    private UUID id;
    private UUID vehicleId;
    private String itemName;
    private Integer intervalMonths;
    private Integer intervalKm;
    private LocalDate lastPerformedAt;
    private Integer lastPerformedKm;
    private LocalDate nextDueAt;
    private Integer nextDueKm;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public VehicleMaintenanceSchedule() {}
    public VehicleMaintenanceSchedule(UUID id, UUID vehicleId, String itemName,
                                      Integer intervalMonths, Integer intervalKm,
                                      LocalDate lastPerformedAt, Integer lastPerformedKm,
                                      LocalDate nextDueAt, Integer nextDueKm, String status,
                                      Instant createdAt, Instant updatedAt) {
        this.id = id; this.vehicleId = vehicleId; this.itemName = itemName;
        this.intervalMonths = intervalMonths; this.intervalKm = intervalKm;
        this.lastPerformedAt = lastPerformedAt; this.lastPerformedKm = lastPerformedKm;
        this.nextDueAt = nextDueAt; this.nextDueKm = nextDueKm; this.status = status;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public Integer getIntervalMonths() { return intervalMonths; }
    public void setIntervalMonths(Integer intervalMonths) { this.intervalMonths = intervalMonths; }
    public Integer getIntervalKm() { return intervalKm; }
    public void setIntervalKm(Integer intervalKm) { this.intervalKm = intervalKm; }
    public LocalDate getLastPerformedAt() { return lastPerformedAt; }
    public void setLastPerformedAt(LocalDate lastPerformedAt) { this.lastPerformedAt = lastPerformedAt; }
    public Integer getLastPerformedKm() { return lastPerformedKm; }
    public void setLastPerformedKm(Integer lastPerformedKm) { this.lastPerformedKm = lastPerformedKm; }
    public LocalDate getNextDueAt() { return nextDueAt; }
    public void setNextDueAt(LocalDate nextDueAt) { this.nextDueAt = nextDueAt; }
    public Integer getNextDueKm() { return nextDueKm; }
    public void setNextDueKm(Integer nextDueKm) { this.nextDueKm = nextDueKm; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
