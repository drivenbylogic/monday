package com.monday.app.project.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public class DeepWorkSessionRequest {

    private String projectId;

    @NotNull(message = "durationMinutes is required")
    @Min(value = 1, message = "durationMinutes must be greater than 0")
    private Integer durationMinutes;

    private Instant startedAt;
    private Instant endedAt;
    private Integer focusScore;
    private String notes;
    private List<String> tasksCompleted;

    public DeepWorkSessionRequest() {}

    public DeepWorkSessionRequest(String projectId, Integer durationMinutes, Instant startedAt,
                                  Instant endedAt, Integer focusScore, String notes, List<String> tasksCompleted) {
        this.projectId = projectId;
        this.durationMinutes = durationMinutes;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.focusScore = focusScore;
        this.notes = notes;
        this.tasksCompleted = tasksCompleted;
    }

    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }

    public Integer getFocusScore() { return focusScore; }
    public void setFocusScore(Integer focusScore) { this.focusScore = focusScore; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<String> getTasksCompleted() { return tasksCompleted; }
    public void setTasksCompleted(List<String> tasksCompleted) { this.tasksCompleted = tasksCompleted; }
}
