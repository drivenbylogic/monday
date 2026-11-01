package com.monday.app.administration.dto;

import java.util.UUID;

public record HabitInsightDto(
        String id,
        String type, // IMPROVEMENT, DROP_OFF, PATTERN, STREAK, TIME_OF_DAY
        String title,
        String message,
        String impact, // POSITIVE, WARNING, NEUTRAL
        UUID habitId
) {}

