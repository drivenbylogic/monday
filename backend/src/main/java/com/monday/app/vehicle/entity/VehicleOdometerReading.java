package com.monday.app.vehicle.entity;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class VehicleOdometerReading {
    private UUID id;
    private UUID vehicleId;
    private LocalDate readingDate;
    private int odometerKm;
    private String source; // MANUAL, SERVICE, FUEL_LOG, TELEMETRY, INITIAL
    private Boolean isCorrection;
    private String notes;
    private Instant createdAt;

    public VehicleOdometerReading() {}

    public VehicleOdometerReading(UUID id, UUID vehicleId, LocalDate readingDate, int odometerKm,
                                  String source, Boolean isCorrection, String notes, Instant createdAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.readingDate = readingDate;
        this.odometerKm = odometerKm;
        this.source = source != null ? source : "MANUAL";
        this.isCorrection = Boolean.TRUE.equals(isCorrection);
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }
    public LocalDate getReadingDate() { return readingDate; }
    public void setReadingDate(LocalDate readingDate) { this.readingDate = readingDate; }
    public int getOdometerKm() { return odometerKm; }
    public void setOdometerKm(int odometerKm) { this.odometerKm = odometerKm; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Boolean getIsCorrection() { return isCorrection; }
    public void setIsCorrection(Boolean correction) { isCorrection = correction; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
