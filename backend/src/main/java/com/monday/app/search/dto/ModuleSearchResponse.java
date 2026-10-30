package com.monday.app.search.dto;

import java.util.ArrayList;
import java.util.List;

public class ModuleSearchResponse {
    private String moduleId;
    private String moduleLabel;
    private String query;
    private int totalCount;
    private long executionTimeMs;
    private List<SearchResultDto> results = new ArrayList<>();

    public ModuleSearchResponse() {}

    public ModuleSearchResponse(String moduleId, String moduleLabel, String query,
                                int totalCount, long executionTimeMs, List<SearchResultDto> results) {
        this.moduleId = moduleId;
        this.moduleLabel = moduleLabel;
        this.query = query;
        this.totalCount = totalCount;
        this.executionTimeMs = executionTimeMs;
        this.results = results != null ? results : new ArrayList<>();
    }

    public String getModuleId() { return moduleId; }
    public void setModuleId(String moduleId) { this.moduleId = moduleId; }

    public String getModuleLabel() { return moduleLabel; }
    public void setModuleLabel(String moduleLabel) { this.moduleLabel = moduleLabel; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public List<SearchResultDto> getResults() { return results; }
    public void setResults(List<SearchResultDto> results) {
        this.results = results != null ? results : new ArrayList<>();
        this.totalCount = this.results.size();
    }
}
