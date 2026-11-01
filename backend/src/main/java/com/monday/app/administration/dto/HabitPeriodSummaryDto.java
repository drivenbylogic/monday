package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HabitPeriodSummaryDto(
        UUID habitId,
        String habitName,
        String category,
        int scheduledCount,
        int completedCount,
        int partialCount,
        int missedCount,
        int skippedCount,
        double completionRate,
        BigDecimal totalQuantity,
        String targetUnit
) {}

