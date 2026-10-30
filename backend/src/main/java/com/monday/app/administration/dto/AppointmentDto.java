package com.monday.app.administration.dto;

import java.time.Instant;
import java.util.UUID;

public record AppointmentDto(
        UUID id,
        String title,
        String description,
        String eventType,
        Instant startAt,
        Instant endAt,
        boolean allDay,
        String location,
        String status, // SCHEDULED, COMPLETED, CANCELLED, RESCHEDULED
        boolean requiresPrep,
        String prepTitle,
        String prepDueText,
        long daysRemaining,
        boolean isUpcoming,
        ChecklistDto checklist,
        Instant createdAt,
        Instant updatedAt
) {}
