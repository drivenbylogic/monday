package com.monday.app.administration.dto;

import java.time.LocalDate;
import java.util.List;

public record CalendarDateDetailDto(
        LocalDate date,
        int eligibleCount,
        int completedCount,
        int partialCount,
        int missedCount,
        int skippedCount,
        double completionPercentage,
        List<HabitOccurrenceDto> occurrences,
        List<HabitLogDto> logs
) {}

