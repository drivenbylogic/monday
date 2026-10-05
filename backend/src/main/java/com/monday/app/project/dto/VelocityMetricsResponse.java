package com.monday.app.project.dto;

import java.util.Map;

public class VelocityMetricsResponse {
    private Map<String, Integer> currentWeekDailyCompletions;
    private Map<String, Integer> pastWeekDailyCompletions;

    public Map<String, Integer> getCurrentWeekDailyCompletions() { return currentWeekDailyCompletions; }
    public void setCurrentWeekDailyCompletions(Map<String, Integer> currentWeekDailyCompletions) { this.currentWeekDailyCompletions = currentWeekDailyCompletions; }

    public Map<String, Integer> getPastWeekDailyCompletions() { return pastWeekDailyCompletions; }
    public void setPastWeekDailyCompletions(Map<String, Integer> pastWeekDailyCompletions) { this.pastWeekDailyCompletions = pastWeekDailyCompletions; }
}
