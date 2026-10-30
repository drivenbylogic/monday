package com.monday.app.agenda.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Bounded typed payload for AI and deterministic agenda synthesis.
 * Unifies context across all operational domains:
 * Schedule, Execution, Finances, Capacity, Vehicles, Administration, Career, and Intelligence.
 */
public record AgendaContextPayload(
        LocalDate agendaDate,
        String timezone,
        ScheduleContext schedule,
        ExecutionContext execution,
        FinancesContext finances,
        WorkCapacityDto capacity,
        VehiclesContext vehicles,
        AdministrationContext administration,
        CareerContext career,
        IntelligenceContext intelligence
) {
    public AgendaContextPayload(
            LocalDate agendaDate,
            String timezone,
            ScheduleContext schedule,
            ExecutionContext execution,
            FinancesContext finances,
            WorkCapacityDto capacity
    ) {
        this(agendaDate, timezone, schedule, execution, finances, capacity, null, null, null, null);
    }
    public record ScheduleContext(
            List<EventSummaryDto> events,
            List<Map<String, Object>> reminders,
            List<EventPrepItemDto> preparationItems,
            List<ScheduleConflictDto> conflicts
    ) {}

    public record ExecutionContext(
            List<TaskSummaryDto> overdueTasks,
            List<TaskSummaryDto> tasksDueToday,
            List<TaskSummaryDto> highPriorityTasks,
            List<ProjectProgressSummaryDto> projects,
            List<MilestoneSummaryDto> milestones,
            DeepWorkSummaryDto recentDeepWork
    ) {}

    public record FinancesContext(
            List<CommitmentSummaryDto> upcomingCommitments,
            List<CreditCardSummaryDto> creditCardDeadlines,
            CreditUtilizationSummaryDto creditUtilization,
            BudgetSpendingDto budgetSummary,
            List<InvestmentSipSummaryDto> upcomingContributions
    ) {}

    public record VehiclesContext(
            int totalVehicles,
            String primaryVehicleName,
            Integer primaryVehicleKm,
            List<VehicleAgendaSummaryDto.VehicleMaintenanceItemDto> upcomingMaintenance,
            List<VehicleAgendaSummaryDto.VehicleDocExpiryDto> expiringDocuments,
            List<VehicleAgendaSummaryDto.VehicleTaskSummaryDto> pendingTasks,
            List<VehicleAgendaSummaryDto.VehicleObligationSummaryDto> upcomingObligations
    ) {}

    public record AdministrationContext(
            int activeObligationsCount,
            int obligationsDueSoonCount,
            int obligationsOverdueCount,
            List<AdministrationAgendaSummaryDto.AdminObligationSummaryDto> upcomingObligations,
            List<AdministrationAgendaSummaryDto.AdminImportantDateSummaryDto> upcomingDates,
            List<AdministrationAgendaSummaryDto.AdminMaintenanceSummaryDto> upcomingMaintenance,
            List<AdministrationAgendaSummaryDto.AdminAppointmentSummaryDto> upcomingAppointments,
            int pendingChecklistItemsCount
    ) {}

    public record CareerContext(
            boolean hasActivePlan,
            String planName,
            String planDescription,
            LocalDate targetDate,
            Long daysToTarget,
            double progressPercent,
            List<CareerAgendaSummaryDto.CareerMilestoneSummaryDto> activeMilestones,
            List<CareerAgendaSummaryDto.CareerTaskSummaryDto> focusTasks
    ) {}

    public record IntelligenceContext(
            long articlesDiscoveredToday,
            int activeFeedNodes,
            List<IntelligenceAgendaSummaryDto.IntelligenceArticleSummaryDto> curatedArticles
    ) {}
}
