package com.monday.app.project.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class MilestoneRoadmapDto {
    private String id;
    private String projectId;
    private String name;
    private String title;
    private String description;
    private String detail;
    private String dueDate;
    private Instant dueAt;
    private String status;
    private String priority;
    private List<MilestoneTaskDto> tasks = new ArrayList<>();

    public MilestoneRoadmapDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
        if (this.title == null) this.title = name;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        this.title = title;
        if (this.name == null) this.name = title;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public List<MilestoneTaskDto> getTasks() { return tasks; }
    public void setTasks(List<MilestoneTaskDto> tasks) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
    }
}

