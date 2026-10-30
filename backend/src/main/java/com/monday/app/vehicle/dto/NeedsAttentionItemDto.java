package com.monday.app.vehicle.dto;

import java.time.LocalDate;

public record NeedsAttentionItemDto(
        String id,
        String type, // MAINTENANCE, DOCUMENT, INSURANCE, OBLIGATION, TASK
        String title,
        String reason,
        String thresholdText,
        String severity, // HIGH, MEDIUM, LOW
        String actionKey, // e.g. "VIEW_DOCUMENTS", "VIEW_MAINTENANCE", "VIEW_INSURANCE", "VIEW_TASKS"
        String actionTargetId,
        LocalDate dueDate,
        Integer dueKm
) {}
