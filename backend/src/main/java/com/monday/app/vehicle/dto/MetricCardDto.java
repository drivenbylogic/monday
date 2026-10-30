package com.monday.app.vehicle.dto;

import java.math.BigDecimal;

public record MetricCardDto(
        int maintenanceDueCount,
        int maintenanceOverdueCount,
        int documentsExpiringCount,
        int documentsExpiredCount,
        boolean insuranceRenewalDue,
        Integer insuranceDaysRemaining,
        BigDecimal fuelSpendingThisMonth,
        BigDecimal totalExpensesThisMonth,
        int currentOdometerKm,
        Double latestFuelEfficiencyKmPerL,
        int pendingTasksCount,
        int pendingObligationsCount
) {}
