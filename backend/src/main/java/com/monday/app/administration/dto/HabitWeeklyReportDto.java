package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public record HabitWeeklyReportDto(
        LocalDate weekStart,
        LocalDate weekEnd,
        int totalEligible,
        int completedCount,
        int partialCount,
        int missedCount,
        int skippedCount,
        double completionPercentage,
        double previousWeekPercentage,
        double percentageDelta,
        String bestPerformingHabit,
        String leastConsistentHabit,
        int perfectDaysCount,
        Map<String, BigDecimal> measurableOutputByUnit,
        List<HabitPeriodSummaryDto> habitSummaries
) {}

