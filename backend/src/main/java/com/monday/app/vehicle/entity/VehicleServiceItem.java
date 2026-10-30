package com.monday.app.vehicle.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class VehicleServiceItem {
    private UUID id;
    private UUID serviceRecordId;
    private String itemName;
    private String category;
    private String brandSpec;
    private BigDecimal quantity;
    private String unit;
    private BigDecimal itemCost;
    private Integer replacementIntervalMonths;
    private Integer replacementIntervalKm;
    private String notes;
    private Instant createdAt;

    public VehicleServiceItem() {}

    public VehicleServiceItem(UUID id, UUID serviceRecordId, String itemName, String category,
                              String brandSpec, BigDecimal quantity, String unit, BigDecimal itemCost,
                              Integer replacementIntervalMonths, Integer replacementIntervalKm,
                              String notes, Instant createdAt) {
        this.id = id;
        this.serviceRecordId = serviceRecordId;
        this.itemName = itemName;
        this.category = category;
        this.brandSpec = brandSpec;
        this.quantity = quantity;
        this.unit = unit;
        this.itemCost = itemCost != null ? itemCost : BigDecimal.ZERO;
        this.replacementIntervalMonths = replacementIntervalMonths;
        this.replacementIntervalKm = replacementIntervalKm;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getServiceRecordId() { return serviceRecordId; }
    public void setServiceRecordId(UUID serviceRecordId) { this.serviceRecordId = serviceRecordId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBrandSpec() { return brandSpec; }
    public void setBrandSpec(String brandSpec) { this.brandSpec = brandSpec; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getItemCost() { return itemCost; }
    public void setItemCost(BigDecimal itemCost) { this.itemCost = itemCost; }
    public Integer getReplacementIntervalMonths() { return replacementIntervalMonths; }
    public void setReplacementIntervalMonths(Integer replacementIntervalMonths) { this.replacementIntervalMonths = replacementIntervalMonths; }
    public Integer getReplacementIntervalKm() { return replacementIntervalKm; }
    public void setReplacementIntervalKm(Integer replacementIntervalKm) { this.replacementIntervalKm = replacementIntervalKm; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
