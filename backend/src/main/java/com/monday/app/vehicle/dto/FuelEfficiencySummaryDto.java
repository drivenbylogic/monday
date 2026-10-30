package com.monday.app.vehicle.dto;

import java.math.BigDecimal;

public record FuelEfficiencySummaryDto(
        Double latestEfficiencyKmPerL,
        Double averageEfficiencyKmPerL,
        BigDecimal totalFuelVolumeLiters,
        BigDecimal totalFuelCost,
        BigDecimal averagePricePerLiter,
        int totalFillUps,
        int fullTankFillUps,
        BigDecimal costPerKm,
        boolean hasSufficientData,
        String dataQualityNote
) {}
