package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record HabitMonthlyReportDto(
        int year,
        int month,
        double completionPercentage,
        double previousMonthPercentage,
        double percentageDelta,
        int totalCompleted,
        int totalPartial,
        int totalMissed,
        int totalSkipped,
        int perfectDaysCount,
        int perfectWeeksCount,
        String bestPerformingHabit,
        String leastConsistentHabit,
        String mostImprovedHabit,
        Map<String, BigDecimal> measurableOutputByUnit,
        List<WeekTrendItemDto> weeklyBreakdown,
        List<HabitPeriodSummaryDto> habitSummaries
) {}

