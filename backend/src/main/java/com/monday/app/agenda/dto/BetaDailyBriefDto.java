package com.monday.app.agenda.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Structured AI Daily Briefing DTO representing the 7 briefing sections:
 * 1. TODAY AT A GLANCE
 * 2. PRIORITIES
 * 3. SCHEDULE
 * 4. PROJECTS AND TASKS
 * 5. FINANCES AND VEHICLES
 * 6. INTELLIGENCE
 * 7. NEXT ACTIONS
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BetaDailyBriefDto(
        @JsonProperty("todayAtAGlance")
        TodayAtAGlanceSection todayAtAGlance,

        @JsonProperty("priorities")
        List<PriorityItem> priorities,

        @JsonProperty("schedule")
        List<ScheduleItem> schedule,

        @JsonProperty("projectsAndTasks")
        List<ProjectTaskItem> projectsAndTasks,

        @JsonProperty("financesAndVehicles")
        List<FinanceVehicleItem> financesAndVehicles,

        @JsonProperty("intelligence")
        List<IntelligenceItem> intelligence,

        @JsonProperty("nextActions")
        List<NextActionItem> nextActions
) {

    public record TodayAtAGlanceSection(
            String headline,
            String summary,
            String urgencyLevel, // NORMAL, ATTENTION, CRITICAL
            String keyFocus
    ) {}

    public record PriorityItem(
            String id,
            String title,
            String whyItMatters,
            String urgency, // URGENT, HIGH, MEDIUM
            String category, // TASK, PROJECT, FINANCE, VEHICLE, SCHEDULE, BLOCKER
            String relatedEntityId,
            String actionLabel
    ) {}

    public record ScheduleItem(
            String id,
            String time,
            String title,
            String duration,
            boolean isConflict,
            String conflictWarning,
            String prepNotes
    ) {}

    public record ProjectTaskItem(
            String id,
            String title,
            String projectName,
            String itemType, // MILESTONE, BLOCKER, APPROACHING_DEADLINE, PROGRESS
            String deadline,
            String details
    ) {}

    public record FinanceVehicleItem(
            String id,
            String title,
            String type, // FINANCE, VEHICLE
            String amountOrImpact,
            String dueDate,
            String urgency, // HIGH, MEDIUM, LOW
            String status, // DUE_TODAY, OVERDUE, UPCOMING, EXPIRING
            String details
    ) {}

    public record IntelligenceItem(
            String id,
            String title,
            String whyItMatters,
            String sourceUrl,
            String sourceName,
            String areaOfInterest
    ) {}

    public record NextActionItem(
            int order,
            String action,
            String context,
            String category // EXECUTION, FINANCIAL, FLEET, SCHEDULING
    ) {}
}

