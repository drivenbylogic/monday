package com.monday.app.search.dto;

import java.util.HashMap;
import java.util.Map;

public class SearchResultDto {
    private String id;
    private String moduleId;
    private String moduleLabel;
    private String category;
    private String title;
    private String subtitle;
    private String snippet;
    private String status;
    private String badge;
    private String icon;
    private String date;
    private Map<String, Object> metadata = new HashMap<>();

    public SearchResultDto() {}

    public SearchResultDto(String id, String moduleId, String moduleLabel, String category,
                           String title, String subtitle, String snippet, String status,
                           String badge, String icon, String date) {
        this.id = id;
        this.moduleId = moduleId;
        this.moduleLabel = moduleLabel;
        this.category = category;
        this.title = title;
        this.subtitle = subtitle;
        this.snippet = snippet;
        this.status = status;
        this.badge = badge;
        this.icon = icon;
        this.date = date;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }

    public String getModuleLabel() { return moduleLabel; }
    public void setModuleLabel(String moduleLabel) { this.moduleLabel = moduleLabel; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBadge() { return badge; }
    public void setBadge(String badge) { this.badge = badge; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public SearchResultDto addMeta(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        if (value != null) {
            this.metadata.put(key, value);
        }
        return this;
    }
}
