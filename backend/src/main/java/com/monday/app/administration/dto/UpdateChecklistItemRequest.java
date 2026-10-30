package com.monday.app.administration.dto;

public record UpdateChecklistItemRequest(
        String title,
        String description,
        Integer position,
        Boolean completed,
        Boolean isCompleted
) {
    public Boolean resolveCompleted() {
        if (completed != null) return completed;
        if (isCompleted != null) return isCompleted;
        return null;
    }
}
