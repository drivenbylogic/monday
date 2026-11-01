package com.monday.app.administration.dto;

import java.util.UUID;

public record StreakHighlightDto(
        UUID habitId,
        String habitName,
        String category,
        int currentStreak,
        int longestStreak
) {}

