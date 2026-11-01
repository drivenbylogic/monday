package com.monday.app.administration.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HabitGroupDto(
        UUID id,
        UUID userId,
        String name,
        String description,
        int displayOrder,
        int habitCount,
        List<HabitDto> habits,
        Instant createdAt,
        Instant updatedAt
) {}

