package com.monday.app.administration.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PersonDto(
        UUID id,
        String displayName,
        String relationshipLabel,
        LocalDate birthday,
        String contactInformation,
        String notes,
        int occasionsCount,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt
) {}
