package com.monday.app.agenda.dto;

import java.time.Instant;
import java.util.UUID;

public record MilestoneSummaryDto(
        UUID id,
        UUID projectId,
        String projectName,
        String name,
        String description,
        String status,
        String priority,
        Instant dueAt
) {}
