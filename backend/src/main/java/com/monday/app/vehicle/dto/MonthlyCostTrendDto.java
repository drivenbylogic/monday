package com.monday.app.vehicle.dto;

import java.math.BigDecimal;

public record MonthlyCostTrendDto(
        String month, // "2026-05", "2026-06", etc.
        BigDecimal fuelCost,
        BigDecimal serviceCost,
        BigDecimal insuranceCost,
        BigDecimal obligationsCost,
        BigDecimal totalCost
) {}
