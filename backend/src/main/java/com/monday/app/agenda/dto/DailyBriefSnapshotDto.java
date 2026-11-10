package com.monday.app.agenda.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Daily Brief input contract snapshot payload.
 * Assembles a compact, validated snapshot of the user's relevant data across 6 operational modules
 * before passing to the AI gateway or deterministic synthesizer.
 */
public record DailyBriefSnapshotDto(
        @JsonProperty("brief_date")
        LocalDate briefDate,

        @JsonProperty("generated_at")
        Instant generatedAt,

        @JsonProperty("timezone")
        String timezone,

        @JsonProperty("modules")
        ModulesSnapshot modules
) {

    public record ModulesSnapshot(
            @JsonProperty("calendar")
            CalendarModule calendar,

            @JsonProperty("projects")
            ProjectsModule projects,

            @JsonProperty("tasks")
            TasksModule tasks,

            @JsonProperty("finances")
            FinancesModule finances,

            @JsonProperty("vehicles")
            VehiclesModule vehicles,

            @JsonProperty("intelligence")
            IntelligenceModule intelligence
    ) {}

    public record CalendarModule(
            @JsonProperty("availability")
            String availability, // AVAILABLE, UNAVAILABLE, INCOMPLETE

            @JsonProperty("events_today")
            List<EventBriefItem> eventsToday,

            @JsonProperty("upcoming_commitments")
            List<CalendarCommitmentBriefItem> upcomingCommitments
    ) {}

    public record EventBriefItem(
            String id,
            String title,
            String startTime,
            String endTime,
            boolean isAllDay,
            String location,
            boolean hasConflict,
            String prepNotes
    ) {}

    public record CalendarCommitmentBriefItem(
            String id,
            String title,
            String date,
            String time,
            String type
    ) {}

    public record ProjectsModule(
            @JsonProperty("availability")
            String availability,

            @JsonProperty("active_projects")
            List<ActiveProjectBriefItem> activeProjects,

            @JsonProperty("upcoming_milestones")
            List<UpcomingMilestoneBriefItem> upcomingMilestones,

            @JsonProperty("blocked_items")
            List<BlockedBriefItem> blockedItems
    ) {}

    public record ActiveProjectBriefItem(
            String id,
            String name,
            String status,
            String healthStatus,
            double progressPercent,
            String keyCategory
    ) {}

    public record UpcomingMilestoneBriefItem(
            String id,
            String projectId,
            String projectName,
            String title,
            LocalDate dueDate,
            String status
    ) {}

    public record BlockedBriefItem(
            String id,
            String type, // TASK, MILESTONE, PROJECT
            String title,
            String projectName,
            String blockerReason
    ) {}

    public record TasksModule(
            @JsonProperty("availability")
            String availability,

            @JsonProperty("overdue")
            List<TaskBriefItem> overdue,

            @JsonProperty("due_today")
            List<TaskBriefItem> dueToday,

            @JsonProperty("upcoming")
            List<TaskBriefItem> upcoming
    ) {}

    public record TaskBriefItem(
            String id,
            String title,
            String projectName,
            LocalDate dueDate,
            String priority,
            Integer estimatedMinutes,
            Long daysOverdue,
            String status
    ) {}

    public record FinancesModule(
            @JsonProperty("availability")
            String availability,

            @JsonProperty("authoritative_metrics")
            Map<String, Object> authoritativeMetrics,

            @JsonProperty("upcoming_obligations")
            List<FinancialObligationBriefItem> upcomingObligations,

            @JsonProperty("budget_alerts")
            List<BudgetAlertBriefItem> budgetAlerts
    ) {}

    public record FinancialObligationBriefItem(
            String id,
            String title,
            String amount,
            LocalDate dueDate,
            boolean isOverdue,
            String category,
            String urgency
    ) {}

    public record BudgetAlertBriefItem(
            String category,
            String spent,
            String budgetLimit,
            double utilizationPercent,
            String alertMessage
    ) {}

    public record VehiclesModule(
            @JsonProperty("availability")
            String availability,

            @JsonProperty("upcoming_maintenance")
            List<VehicleMaintenanceBriefItem> upcomingMaintenance,

            @JsonProperty("expiring_documents")
            List<VehicleDocExpiryBriefItem> expiringDocuments,

            @JsonProperty("insurance_and_obligations")
            List<VehicleObligationBriefItem> insuranceAndObligations
    ) {}

    public record VehicleMaintenanceBriefItem(
            String id,
            String vehicleName,
            String itemName,
            LocalDate nextDueAt,
            Integer nextDueKm,
            boolean isOverdue
    ) {}

    public record VehicleDocExpiryBriefItem(
            String id,
            String vehicleName,
            String docType,
            String title,
            LocalDate expiryDate,
            long daysRemaining,
            boolean isExpired
    ) {}

    public record VehicleObligationBriefItem(
            String id,
            String vehicleName,
            String title,
            String amount,
            LocalDate dueDate
    ) {}

    public record IntelligenceModule(
            @JsonProperty("availability")
            String availability,

            @JsonProperty("articles")
            List<IntelligenceArticleBriefItem> articles
    ) {}

    public record IntelligenceArticleBriefItem(
            String id,
            String title,
            String sourceName,
            String sourceUrl,
            String areaOfInterest,
            Instant publishedAt,
            String summarySnippet
    ) {}
}

