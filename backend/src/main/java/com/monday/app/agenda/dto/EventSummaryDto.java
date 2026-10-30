package com.monday.app.agenda.dto;

import java.time.Instant;
import java.util.UUID;

public record EventSummaryDto(
        UUID id,
        String title,
        String description,
        String eventType,
        Instant startAt,
        Instant endAt,
        boolean allDay,
        String location,
        String status,
        boolean requiresPrep,
        String prepTitle,
        String prepDueText
) {}
