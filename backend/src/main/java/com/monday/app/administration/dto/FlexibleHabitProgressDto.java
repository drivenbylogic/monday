package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record FlexibleHabitProgressDto(
        UUID habitId,
        String habitName,
        String category,
        int targetCount,
        int completedCount,
        int remainingCount,
        double percentage,
        String unit,
        BigDecimal targetValue,
        boolean loggedToday,
        List<HabitLogDto> logsThisWeek
) {}

