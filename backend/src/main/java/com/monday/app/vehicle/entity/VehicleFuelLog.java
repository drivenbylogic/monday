package com.monday.app.vehicle.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class VehicleFuelLog {
    private UUID id;
    private UUID vehicleId;
    private LocalDate logDate;
    private String brand;
    private String fuelBrand;
    private String location;
    private String stationLocation;
    private String fuelGrade;
    private Boolean fullTank;
    private BigDecimal volumeLiters;
    private BigDecimal totalCost;
    private BigDecimal pricePerLiter;
    private Integer odometerKm;
    private BigDecimal tripEfficiency;
    private Instant createdAt;

    public VehicleFuelLog() {}

    public VehicleFuelLog(UUID id, UUID vehicleId, LocalDate logDate, String brand, String fuelBrand,
                          String location, String stationLocation, String fuelGrade, Boolean fullTank,
                          BigDecimal volumeLiters, BigDecimal totalCost, BigDecimal pricePerLiter,
                          Integer odometerKm, BigDecimal tripEfficiency, Instant createdAt) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.logDate = logDate;
        this.brand = brand;
        this.fuelBrand = fuelBrand;
        this.location = location;
        this.stationLocation = stationLocation;
        this.fuelGrade = fuelGrade;
        this.fullTank = fullTank;
        this.volumeLiters = volumeLiters;
        this.totalCost = totalCost;
        this.pricePerLiter = pricePerLiter;
        this.odometerKm = odometerKm;
        this.tripEfficiency = tripEfficiency;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getVehicleId() { return vehicleId; }
    public void setVehicleId(UUID vehicleId) { this.vehicleId = vehicleId; }
    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getFuelBrand() { return fuelBrand; }
    public void setFuelBrand(String fuelBrand) { this.fuelBrand = fuelBrand; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStationLocation() { return stationLocation; }
    public void setStationLocation(String stationLocation) { this.stationLocation = stationLocation; }
    public String getFuelGrade() { return fuelGrade; }
    public void setFuelGrade(String fuelGrade) { this.fuelGrade = fuelGrade; }
    public Boolean getFullTank() { return fullTank; }
    public void setFullTank(Boolean fullTank) { this.fullTank = fullTank; }
    public BigDecimal getVolumeLiters() { return volumeLiters; }
    public void setVolumeLiters(BigDecimal volumeLiters) { this.volumeLiters = volumeLiters; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public BigDecimal getPricePerLiter() { return pricePerLiter; }
    public void setPricePerLiter(BigDecimal pricePerLiter) { this.pricePerLiter = pricePerLiter; }
    public Integer getOdometerKm() { return odometerKm; }
    public void setOdometerKm(Integer odometerKm) { this.odometerKm = odometerKm; }
    public BigDecimal getTripEfficiency() { return tripEfficiency; }
    public void setTripEfficiency(BigDecimal tripEfficiency) { this.tripEfficiency = tripEfficiency; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
