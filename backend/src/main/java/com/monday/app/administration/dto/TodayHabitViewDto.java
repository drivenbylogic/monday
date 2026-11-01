package com.monday.app.administration.dto;

import java.time.LocalDate;
import java.util.List;

public record TodayHabitViewDto(
        LocalDate date,
        int totalScheduled,
        int completedCount,
        int partialCount,
        int pendingCount,
        int missedCount,
        int skippedCount,
        double completionPercentage,
        List<HabitOccurrenceDto> pendingHabits,
        List<HabitOccurrenceDto> completedHabits,
        List<FlexibleHabitProgressDto> flexibleHabits
) {}

