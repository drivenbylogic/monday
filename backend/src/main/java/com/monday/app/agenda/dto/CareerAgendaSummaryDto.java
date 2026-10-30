package com.monday.app.agenda.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Programmatic dashboard and agenda context for the Career module.
 */
public record CareerAgendaSummaryDto(
        boolean hasActivePlan,
        String planId,
        String planName,
        String planDescription,
        LocalDate startDate,
        LocalDate targetDate,
        Long daysToTarget,
        int totalMilestones,
        int completedMilestones,
        int totalTasks,
        int completedTasks,
        double progressPercent,
        List<CareerMilestoneSummaryDto> activeMilestones,
        List<CareerTaskSummaryDto> upcomingCareerTasks
) {
    public record CareerMilestoneSummaryDto(
            String id,
            String name,
            LocalDate dueDate,
            String priority,
            double progressPercent,
            int taskCount
    ) {}

    public record CareerTaskSummaryDto(
            String id,
            String milestoneId,
            String milestoneName,
            String title,
            String priority,
            LocalDate dueDate,
            boolean isCompleted
    ) {}
}
