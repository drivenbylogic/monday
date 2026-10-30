package com.monday.app.agenda.dto;

import java.util.List;

public record TaskOverviewDto(
        int dueTodayCount,
        int overdueCount,
        int highPriorityCount,
        int inProgressCount,
        int totalEstimatedMinutes,
        List<TaskSummaryDto> dueTodayTasks,
        List<TaskSummaryDto> overdueTasks,
        List<TaskSummaryDto> highPriorityTasks,
        List<TaskSummaryDto> inProgressTasks
) {}
