package com.monday.app.administration.dto;

import java.time.LocalDate;

public record WeekDaySummaryDto(
        LocalDate date,
        String dayOfWeek,
        int eligibleCount,
        int completedCount,
        int partialCount,
        int missedCount,
        int skippedCount,
        double percentage
) {}

