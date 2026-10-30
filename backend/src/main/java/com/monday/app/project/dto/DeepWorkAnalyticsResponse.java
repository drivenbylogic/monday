package com.monday.app.project.dto;

import java.util.Map;

public record DeepWorkAnalyticsResponse(
        Double currentWeekHours,
        Double pastWeekHours,
        Double percentageChange,
        Map<String, Double> dailyAverages,
        String leadArchitect
) {}
