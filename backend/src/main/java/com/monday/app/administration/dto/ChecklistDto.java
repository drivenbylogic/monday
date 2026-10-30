package com.monday.app.administration.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChecklistDto(
        UUID id,
        String title,
        String description,
        String administrationRecordType, // OBLIGATION, PERSON, IMPORTANT_DATE, GADGET, MAINTENANCE, APPOINTMENT
        UUID administrationRecordId,
        String status, // ACTIVE, COMPLETED, ARCHIVED
        List<ChecklistItemDto> items,
        int completedCount,
        int totalCount,
        int progressPercent,
        Instant createdAt,
        Instant updatedAt
) {}
