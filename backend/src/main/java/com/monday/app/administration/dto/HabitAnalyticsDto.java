package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.util.List;

public record HabitAnalyticsDto(
        HabitDto habit,
        int currentStreak,
        int longestStreak,
        double completionRate7Days,
        double completionRate30Days,
        double completionRate90Days,
        BigDecimal totalAccumulatedQuantity,
        BigDecimal averageQuantityPerCompleted,
        int totalCompleted,
        int totalPartial,
        int totalMissed,
        int totalSkipped,
        List<HabitOccurrenceDto> recentActivity,
        List<WeekTrendItemDto> weeklyTrend
) {}

