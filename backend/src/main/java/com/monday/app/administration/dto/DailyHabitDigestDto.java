package com.monday.app.administration.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyHabitDigestDto(
        LocalDate date,
        int totalScheduled,
        List<HabitOccurrenceDto> morningHabits,
        List<HabitOccurrenceDto> eveningHabits,
        List<HabitOccurrenceDto> remainingHabits,
        double completionPercentage,
        String message
) {}

