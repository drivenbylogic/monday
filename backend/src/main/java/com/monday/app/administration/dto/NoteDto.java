package com.monday.app.administration.dto;

import java.time.Instant;
import java.util.UUID;

public record NoteDto(
        UUID id,
        String title,
        String body,
        Instant createdAt,
        Instant updatedAt
) {}
