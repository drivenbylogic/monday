package com.monday.app.administration.dto;

import java.time.LocalDate;
import java.util.List;

public record WeekHabitViewDto(
        LocalDate weekStartDate,
        LocalDate weekEndDate,
        int totalEligible,
        int totalCompleted,
        int totalPartial,
        int totalMissed,
        int totalSkipped,
        double completionPercentage,
        List<WeekDaySummaryDto> days,
        List<HabitOccurrenceDto> occurrences,
        List<FlexibleHabitProgressDto> flexibleHabits
) {}

