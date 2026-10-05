package com.monday.app.project.dto;

public class ProjectMetricsResponse {
    private int activeProjectCount;
    private int milestonesCount;
    private int openTasksCount;
    private double activityVelocity;

    public int getActiveProjectCount() { return activeProjectCount; }
    public void setActiveProjectCount(int activeProjectCount) { this.activeProjectCount = activeProjectCount; }

    public int getMilestonesCount() { return milestonesCount; }
    public void setMilestonesCount(int milestonesCount) { this.milestonesCount = milestonesCount; }

    public int getOpenTasksCount() { return openTasksCount; }
    public void setOpenTasksCount(int openTasksCount) { this.openTasksCount = openTasksCount; }

    public double getActivityVelocity() { return activityVelocity; }
    public void setActivityVelocity(double activityVelocity) { this.activityVelocity = activityVelocity; }
}
