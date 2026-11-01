package com.monday.app.administration.dto;

public record WeekTrendItemDto(
        String weekLabel,
        double completionRate,
        int completedCount,
        int targetCount
) {}

