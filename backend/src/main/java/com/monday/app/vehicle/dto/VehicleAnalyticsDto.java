package com.monday.app.vehicle.dto;

import java.util.List;

public record VehicleAnalyticsDto(
        UsageSummaryDto usage,
        FuelEfficiencySummaryDto fuelEfficiency,
        CostSummaryDto costs,
        List<OdometerReadingDto> recentOdometerReadings
) {}
