package com.monday.app.administration.dto;

import java.time.Instant;
import java.util.UUID;

public record ChecklistItemDto(
        UUID id,
        UUID checklistId,
        String title,
        String description,
        int position,
        boolean completed,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {}
