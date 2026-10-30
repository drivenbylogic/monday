package com.monday.app.agenda.dto;

import java.time.Instant;
import java.util.UUID;

public record SuggestedTimeBlockDto(
        Instant startAt,
        Instant endAt,
        String title,
        String description,
        UUID associatedTaskId
) {}
