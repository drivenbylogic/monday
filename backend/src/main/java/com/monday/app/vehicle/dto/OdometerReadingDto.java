package com.monday.app.vehicle.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OdometerReadingDto(
        UUID id,
        UUID vehicleId,
        LocalDate readingDate,
        int odometerKm,
        String source,
        Boolean isCorrection,
        String notes,
        Integer distanceDeltaFromPrevious,
        Instant createdAt
) {}
