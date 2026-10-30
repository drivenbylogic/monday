package com.monday.app.vehicle.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CostSummaryDto(
        BigDecimal totalOperatingCostInPeriod,
        BigDecimal totalCumulativeOperatingCost,
        BigDecimal purchaseAmount,
        BigDecimal totalOwnershipCost, // purchaseAmount + totalCumulativeOperatingCost
        BigDecimal costPerKmInPeriod,
        Map<String, BigDecimal> categoryBreakdown,
        List<MonthlyCostTrendDto> monthlyTrends,
        String selectedPeriod
) {}
