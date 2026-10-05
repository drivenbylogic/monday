package com.monday.app.vehicle.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class Vehicle {
    private UUID id;
    private String name;
    private String registrationNumber;
    private LocalDate purchaseDate;
    private BigDecimal purchaseAmount;
    private int currentKm;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;

    public Vehicle() {}
    public Vehicle(UUID id, String name, String registrationNumber, LocalDate purchaseDate,
                   BigDecimal purchaseAmount, int currentKm, String status,
                   Instant createdAt, Instant updatedAt) {
        this.id = id; this.name = name; this.registrationNumber = registrationNumber;
        this.purchaseDate = purchaseDate; this.purchaseAmount = purchaseAmount;
        this.currentKm = currentKm; this.status = status;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public LocalDate getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(LocalDate purchaseDate) { this.purchaseDate = purchaseDate; }
    public BigDecimal getPurchaseAmount() { return purchaseAmount; }
    public void setPurchaseAmount(BigDecimal purchaseAmount) { this.purchaseAmount = purchaseAmount; }
    public int getCurrentKm() { return currentKm; }
    public void setCurrentKm(int currentKm) { this.currentKm = currentKm; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
