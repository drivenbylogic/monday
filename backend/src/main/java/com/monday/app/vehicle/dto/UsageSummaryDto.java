package com.monday.app.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UsageSummaryDto(
        int currentOdometerKm,
        LocalDate latestReadingDate,
        int distanceCoveredInPeriod,
        int distanceCoveredLast30Days,
        int distanceCoveredLast12Months,
        int totalReadingsRecorded,
        Integer estimatedKmToNextMaintenance,
        String nextMaintenanceItem
) {}
