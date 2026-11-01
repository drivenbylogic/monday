package com.monday.app.administration.dto;

import java.util.List;

public record HabitOverviewDto(
        TodayHabitViewDto today,
        WeekHabitViewDto week,
        int activeHabitCount,
        List<StreakHighlightDto> streakHighlights,
        List<HabitInsightDto> insights
) {}

