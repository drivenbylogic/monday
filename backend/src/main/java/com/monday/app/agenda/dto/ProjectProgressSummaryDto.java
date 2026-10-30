package com.monday.app.agenda.dto;

import java.time.Instant;
import java.util.UUID;

public record ProjectProgressSummaryDto(
        UUID id,
        String name,
        String description,
        String category,
        String status,
        String priority,
        Instant dueAt,
        int totalTasks,
        int completedTasks,
        Integer progressPercent, // null if totalTasks == 0 to avoid misleading 0%
        String nextMilestoneName,
        Instant nextMilestoneDueAt
) {}
