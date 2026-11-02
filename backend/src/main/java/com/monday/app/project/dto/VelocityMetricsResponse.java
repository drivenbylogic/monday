package com.monday.app.project.dto;

import java.util.Map;

public class VelocityMetricsResponse {
    private Map<String, Integer> currentWeekDailyCompletions;
    private Map<String, Integer> pastWeekDailyCompletions;
    private Double velocityHealthScore;
    private Double velocityPercentageChange;
    private Integer currentWeekTasksClosed;
    private Integer previousWeekTasksClosed;

    public Map<String, Integer> getCurrentWeekDailyCompletions() { return currentWeekDailyCompletions; }
    public void setCurrentWeekDailyCompletions(Map<String, Integer> currentWeekDailyCompletions) { this.currentWeekDailyCompletions = currentWeekDailyCompletions; }

    public Map<String, Integer> getPastWeekDailyCompletions() { return pastWeekDailyCompletions; }
    public void setPastWeekDailyCompletions(Map<String, Integer> pastWeekDailyCompletions) { this.pastWeekDailyCompletions = pastWeekDailyCompletions; }

    public Double getVelocityHealthScore() { return velocityHealthScore; }
    public void setVelocityHealthScore(Double velocityHealthScore) { this.velocityHealthScore = velocityHealthScore; }

    public Double getVelocityPercentageChange() { return velocityPercentageChange; }
    public void setVelocityPercentageChange(Double velocityPercentageChange) { this.velocityPercentageChange = velocityPercentageChange; }

    public Integer getCurrentWeekTasksClosed() { return currentWeekTasksClosed; }
    public void setCurrentWeekTasksClosed(Integer currentWeekTasksClosed) { this.currentWeekTasksClosed = currentWeekTasksClosed; }

    public Integer getPreviousWeekTasksClosed() { return previousWeekTasksClosed; }
    public void setPreviousWeekTasksClosed(Integer previousWeekTasksClosed) { this.previousWeekTasksClosed = previousWeekTasksClosed; }
}
