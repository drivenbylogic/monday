package com.monday.app.agenda.service;

import com.monday.app.agenda.dto.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Prepares a bounded, typed context payload for AI or deterministic agenda generation.
 * Never sends unbounded data, passwords, or raw secrets.
 */
@Service
public class AgendaContextService {

    private final HomeDashboardService dashboardService;

    public AgendaContextService(HomeDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    public AgendaContextPayload buildContext(LocalDate date, ZoneId zoneId) {
        HomeDashboardResponse dashboard = dashboardService.getDashboard(date, zoneId);

        // Bound schedule context
        List<EventSummaryDto> boundedEvents = dashboard.schedule().eventsToday();
        List<Map<String, Object>> remindersList = dashboard.schedule().remindersToday().stream()
                .map(r -> Map.<String, Object>of(
                        "id", r.getId() != null ? r.getId().toString() : "",
                        "title", r.getTitle() != null ? r.getTitle() : "",
                        "remindAt", r.getRemindAt() != null ? r.getRemindAt().toString() : ""
                ))
                .toList();

        AgendaContextPayload.ScheduleContext scheduleContext = new AgendaContextPayload.ScheduleContext(
                boundedEvents,
                remindersList,
                dashboard.schedule().incompletePrepItems(),
                dashboard.schedule().conflicts()
        );

        // Bound execution context (top 10 overdue, top 10 due today, top 10 high priority)
        List<TaskSummaryDto> boundedOverdue = dashboard.tasks().overdueTasks().stream().limit(10).toList();
        List<TaskSummaryDto> boundedDueToday = dashboard.tasks().dueTodayTasks().stream().limit(10).toList();
        List<TaskSummaryDto> boundedHighPriority = dashboard.tasks().highPriorityTasks().stream().limit(10).toList();
        List<ProjectProgressSummaryDto> boundedProjects = dashboard.projects().activeProjects().stream().limit(5).toList();
        List<MilestoneSummaryDto> boundedMilestones = dashboard.projects().upcomingMilestones().stream().limit(5).toList();

        AgendaContextPayload.ExecutionContext executionContext = new AgendaContextPayload.ExecutionContext(
                boundedOverdue,
                boundedDueToday,
                boundedHighPriority,
                boundedProjects,
                boundedMilestones,
                dashboard.projects().recentDeepWork()
        );

        // Bound finances context (commitments due today and soon, credit card deadlines, budget summary)
        List<CommitmentSummaryDto> upcomingCommitments = new ArrayList<>(dashboard.finances().dueToday());
        upcomingCommitments.addAll(dashboard.finances().dueSoon());

        AgendaContextPayload.FinancesContext financesContext = new AgendaContextPayload.FinancesContext(
                upcomingCommitments,
                dashboard.finances().creditCards(),
                dashboard.finances().overallCreditUtilization(),
                dashboard.budget(),
                dashboard.finances().upcomingSips()
        );

        // Bound vehicles context
        VehicleAgendaSummaryDto veh = dashboard.vehicles();
        AgendaContextPayload.VehiclesContext vehiclesContext = new AgendaContextPayload.VehiclesContext(
                veh != null ? veh.totalVehicles() : 0,
                veh != null ? veh.primaryVehicleName() : null,
                veh != null ? veh.primaryVehicleKm() : null,
                veh != null ? veh.upcomingMaintenance().stream().limit(5).toList() : Collections.emptyList(),
                veh != null ? veh.expiringDocuments().stream().limit(5).toList() : Collections.emptyList(),
                veh != null ? veh.pendingTasks().stream().limit(5).toList() : Collections.emptyList(),
                veh != null ? veh.upcomingObligations().stream().limit(5).toList() : Collections.emptyList()
        );

        // Bound administration context
        AdministrationAgendaSummaryDto adm = dashboard.administration();
        AgendaContextPayload.AdministrationContext administrationContext = new AgendaContextPayload.AdministrationContext(
                adm != null ? adm.activeObligationsCount() : 0,
                adm != null ? adm.obligationsDueSoonCount() : 0,
                adm != null ? adm.obligationsOverdueCount() : 0,
                adm != null ? adm.upcomingObligations().stream().limit(5).toList() : Collections.emptyList(),
                adm != null ? adm.upcomingDates().stream().limit(5).toList() : Collections.emptyList(),
                adm != null ? adm.upcomingMaintenance().stream().limit(5).toList() : Collections.emptyList(),
                adm != null ? adm.upcomingAppointments().stream().limit(5).toList() : Collections.emptyList(),
                adm != null ? adm.pendingChecklistItemsCount() : 0
        );

        // Bound career context
        CareerAgendaSummaryDto car = dashboard.career();
        AgendaContextPayload.CareerContext careerContext = new AgendaContextPayload.CareerContext(
                car != null && car.hasActivePlan(),
                car != null ? car.planName() : null,
                car != null ? car.planDescription() : null,
                car != null ? car.targetDate() : null,
                car != null ? car.daysToTarget() : null,
                car != null ? car.progressPercent() : 0.0,
                car != null ? car.activeMilestones().stream().limit(3).toList() : Collections.emptyList(),
                car != null ? car.upcomingCareerTasks().stream().limit(5).toList() : Collections.emptyList()
        );

        // Bound intelligence context
        IntelligenceAgendaSummaryDto intel = dashboard.intelligence();
        AgendaContextPayload.IntelligenceContext intelligenceContext = new AgendaContextPayload.IntelligenceContext(
                intel != null ? intel.articlesDiscoveredToday() : 0,
                intel != null ? intel.activeFeedNodes() : 0,
                intel != null ? intel.topDailyArticles().stream().limit(5).toList() : Collections.emptyList()
        );

        return new AgendaContextPayload(
                date,
                zoneId.getId(),
                scheduleContext,
                executionContext,
                financesContext,
                dashboard.capacity(),
                vehiclesContext,
                administrationContext,
                careerContext,
                intelligenceContext
        );
    }
}
