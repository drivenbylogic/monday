package com.monday.app.career.entity;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Career Plan domain entity.
 * Represents an imported career transition project with deep milestones and task hierarchies.
 */
public final class CareerPlan {

    private String id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate targetDate;
    private String color;
    private String viewStyle;
    private String planData;
    private Instant createdAt;
    private Instant updatedAt;

    public CareerPlan() {}

    public CareerPlan(String id, String name, String description, LocalDate startDate, LocalDate targetDate,
                      String color, String viewStyle, String planData, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.targetDate = targetDate;
        this.color = color;
        this.viewStyle = viewStyle;
        this.planData = planData;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getViewStyle() { return viewStyle; }
    public void setViewStyle(String viewStyle) { this.viewStyle = viewStyle; }

    public String getPlanData() { return planData; }
    public void setPlanData(String planData) { this.planData = planData; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
