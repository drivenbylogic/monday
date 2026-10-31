package com.monday.app.project.dto;

import java.time.Instant;

public class MilestoneTaskDto {
    private String id;
    private String milestoneId;
    private String title;
    private String description;
    private String dueTime;
    private Instant dueAt;
    private boolean completed;
    private String status;
    private String priority;

    public MilestoneTaskDto() {}

    public MilestoneTaskDto(String id, String milestoneId, String title, String description,
                            String dueTime, Instant dueAt, boolean completed, String status, String priority) {
        this.id = id;
        this.milestoneId = milestoneId;
        this.title = title;
        this.description = description;
        this.dueTime = dueTime;
        this.dueAt = dueAt;
        this.completed = completed;
        this.status = status;
        this.priority = priority;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMilestoneId() { return milestoneId; }
    public void setMilestoneId(String milestoneId) { this.milestoneId = milestoneId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDueTime() { return dueTime; }
    public void setDueTime(String dueTime) { this.dueTime = dueTime; }

    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}

