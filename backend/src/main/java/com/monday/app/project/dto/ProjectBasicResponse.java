package com.monday.app.project.dto;

import java.util.List;
import java.util.UUID;

public class ProjectBasicResponse {
    private UUID id;
    private String name;
    private String description;
    private String tags; // JSON string
    private int percentageToCompletion;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    
    public int getPercentageToCompletion() { return percentageToCompletion; }
    public void setPercentageToCompletion(int percentageToCompletion) { this.percentageToCompletion = percentageToCompletion; }
}
