package com.monday.app.agenda.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Programmatic dashboard and agenda context for the Administration module.
 */
public record AdministrationAgendaSummaryDto(
        int activeObligationsCount,
        int obligationsDueSoonCount,
        int obligationsOverdueCount,
        BigDecimal monthlySubscriptionCommitment,
        List<AdminObligationSummaryDto> upcomingObligations,
        int upcomingOccasionsCount,
        List<AdminImportantDateSummaryDto> upcomingDates,
        int maintenanceDueCount,
        List<AdminMaintenanceSummaryDto> upcomingMaintenance,
        int upcomingAppointmentsCount,
        List<AdminAppointmentSummaryDto> upcomingAppointments,
        int pendingChecklistItemsCount
) {
    public record AdminObligationSummaryDto(
            UUID id,
            String name,
            String obligationType,
            String category,
            LocalDate nextDueDate,
            BigDecimal expectedAmount,
            String currency,
            boolean isOverdue
    ) {}

    public record AdminImportantDateSummaryDto(
            UUID id,
            String title,
            LocalDate date,
            String dateType,
            String personName,
            long daysRemaining
    ) {}

    public record AdminMaintenanceSummaryDto(
            UUID id,
            String gadgetName,
            String title,
            String maintenanceType,
            LocalDate dueDate,
            boolean isDue
    ) {}

    public record AdminAppointmentSummaryDto(
            UUID id,
            String title,
            Instant startAt,
            String location
    ) {}
}
