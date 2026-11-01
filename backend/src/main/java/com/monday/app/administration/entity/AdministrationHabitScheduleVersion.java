package com.monday.app.administration.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class AdministrationHabitScheduleVersion {

    private UUID id;
    private UUID habitId;
    private String scheduleConfig; // JSON string
    private BigDecimal targetValue;
    private BigDecimal minimumTargetValue;
    private String targetUnit;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;
    private Instant createdAt;

    public AdministrationHabitScheduleVersion() {}

    public AdministrationHabitScheduleVersion(UUID id, UUID habitId, String scheduleConfig,
                                             BigDecimal targetValue, BigDecimal minimumTargetValue,
                                             String targetUnit, LocalDate effectiveFrom,
                                             LocalDate effectiveUntil, Instant createdAt) {
        this.id = id;
        this.habitId = habitId;
        this.scheduleConfig = scheduleConfig;
        this.targetValue = targetValue;
        this.minimumTargetValue = minimumTargetValue;
        this.targetUnit = targetUnit;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getHabitId() { return habitId; }
    public void setHabitId(UUID habitId) { this.habitId = habitId; }

    public String getScheduleConfig() { return scheduleConfig; }
    public void setScheduleConfig(String scheduleConfig) { this.scheduleConfig = scheduleConfig; }

    public BigDecimal getTargetValue() { return targetValue; }
    public void setTargetValue(BigDecimal targetValue) { this.targetValue = targetValue; }

    public BigDecimal getMinimumTargetValue() { return minimumTargetValue; }
    public void setMinimumTargetValue(BigDecimal minimumTargetValue) { this.minimumTargetValue = minimumTargetValue; }

    public String getTargetUnit() { return targetUnit; }
    public void setTargetUnit(String targetUnit) { this.targetUnit = targetUnit; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }

    public LocalDate getEffectiveUntil() { return effectiveUntil; }
    public void setEffectiveUntil(LocalDate effectiveUntil) { this.effectiveUntil = effectiveUntil; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

