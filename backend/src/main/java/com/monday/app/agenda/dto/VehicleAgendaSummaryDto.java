package com.monday.app.agenda.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Programmatic dashboard and agenda context for the Vehicles module.
 */
public record VehicleAgendaSummaryDto(
        int totalVehicles,
        String primaryVehicleName,
        Integer primaryVehicleKm,
        int maintenanceDueCount,
        int maintenanceOverdueCount,
        List<VehicleMaintenanceItemDto> upcomingMaintenance,
        int documentsExpiringCount,
        int documentsExpiredCount,
        List<VehicleDocExpiryDto> expiringDocuments,
        int pendingTasksCount,
        List<VehicleTaskSummaryDto> pendingTasks,
        List<VehicleObligationSummaryDto> upcomingObligations
) {
    public record VehicleMaintenanceItemDto(
            UUID id,
            UUID vehicleId,
            String vehicleName,
            String itemName,
            LocalDate nextDueAt,
            Integer nextDueKm,
            boolean isOverdue
    ) {}

    public record VehicleDocExpiryDto(
            UUID id,
            UUID vehicleId,
            String vehicleName,
            String docType,
            String title,
            LocalDate expiryDate,
            long daysRemaining,
            boolean isExpired
    ) {}

    public record VehicleTaskSummaryDto(
            UUID id,
            UUID vehicleId,
            String vehicleName,
            String title,
            String priority
    ) {}

    public record VehicleObligationSummaryDto(
            UUID id,
            UUID vehicleId,
            String vehicleName,
            String obligationType,
            LocalDate dueDate,
            BigDecimal amount,
            String status
    ) {}
}
