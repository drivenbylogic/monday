package com.monday.app.agenda.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Top-level response for the programmatic home dashboard.
 * Purely deterministic, calculated from database facts without AI inferences.
 * Covers all domains: Schedule, Tasks, Projects, Finances, Budget, Vehicles, Administration, Career, and Intelligence.
 */
public record HomeDashboardResponse(
        LocalDate date,
        ScheduleDashboardDto schedule,
        TaskOverviewDto tasks,
        ProjectOverviewDto projects,
        FinanceObligationsDto finances,
        BudgetSpendingDto budget,
        WorkCapacityDto capacity,
        VehicleAgendaSummaryDto vehicles,
        AdministrationAgendaSummaryDto administration,
        CareerAgendaSummaryDto career,
        IntelligenceAgendaSummaryDto intelligence,
        Instant refreshedAt
) {
    public HomeDashboardResponse(
            LocalDate date,
            ScheduleDashboardDto schedule,
            TaskOverviewDto tasks,
            ProjectOverviewDto projects,
            FinanceObligationsDto finances,
            BudgetSpendingDto budget,
            WorkCapacityDto capacity,
            Instant refreshedAt
    ) {
        this(date, schedule, tasks, projects, finances, budget, capacity, null, null, null, null, refreshedAt);
    }
}
