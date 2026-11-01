package com.monday.app.administration.dto;

import java.time.LocalDate;

public record CalendarDayDto(
        LocalDate date,
        int eligibleCount,
        int completedCount,
        int partialCount,
        int missedCount,
        int skippedCount,
        String ratio, // e.g. "4/5"
        double completionPercentage,
        String statusLevel // HIGH (>=80%), MODERATE (50-79%), LOW (<50%), NEUTRAL (no habits or future)
) {}

