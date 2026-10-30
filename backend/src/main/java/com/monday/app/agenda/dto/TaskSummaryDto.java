package com.monday.app.agenda.dto;

import java.time.Instant;
import java.util.UUID;

public record TaskSummaryDto(
        UUID id,
        String title,
        String description,
        UUID projectId,
        String projectName,
        UUID milestoneId,
        String milestoneName,
        String status,
        String priority,
        Instant dueAt,
        Integer estimatedMinutes,
        boolean isRecurring
) {}
