package com.monday.app.project.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProjectBasicResponse {
    private UUID id;
    private String name;
    private String description;
    private String category;
    private String status;
    private String priority;
    private String tags; // JSON string
    private Instant startAt;
    private Instant dueAt;
    private Instant completedAt;
    private int percentageToCompletion;
    private String nextMilestone;
    private String nextMilestoneDue;
    private String impendingTask;
    private String impendingTaskDue;
    private List<MilestoneRoadmapDto> milestonesRoadmap = new ArrayList<>();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public Instant getStartAt() { return startAt; }
    public void setStartAt(Instant startAt) { this.startAt = startAt; }

    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public int getPercentageToCompletion() { return percentageToCompletion; }
    public void setPercentageToCompletion(int percentageToCompletion) { this.percentageToCompletion = percentageToCompletion; }

    public String getNextMilestone() { return nextMilestone; }
    public void setNextMilestone(String nextMilestone) { this.nextMilestone = nextMilestone; }

    public String getNextMilestoneDue() { return nextMilestoneDue; }
    public void setNextMilestoneDue(String nextMilestoneDue) { this.nextMilestoneDue = nextMilestoneDue; }

    public String getImpendingTask() { return impendingTask; }
    public void setImpendingTask(String impendingTask) { this.impendingTask = impendingTask; }

    public String getImpendingTaskDue() { return impendingTaskDue; }
    public void setImpendingTaskDue(String impendingTaskDue) { this.impendingTaskDue = impendingTaskDue; }

    public List<MilestoneRoadmapDto> getMilestonesRoadmap() { return milestonesRoadmap; }
    public void setMilestonesRoadmap(List<MilestoneRoadmapDto> milestonesRoadmap) {
        this.milestonesRoadmap = milestonesRoadmap != null ? milestonesRoadmap : new ArrayList<>();
    }
}
