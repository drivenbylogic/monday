package com.monday.app.vehicle.dto;

import java.time.Instant;
import java.util.UUID;

public record VehicleTaskDto(
        UUID id,
        UUID vehicleId,
        String title,
        String metaText,
        Boolean completed,
        String priority,
        String linkedModule,
        Instant completedAt,
        Instant createdAt
) {}
