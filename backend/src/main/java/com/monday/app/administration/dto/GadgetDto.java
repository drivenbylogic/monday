package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record GadgetDto(
        UUID id,
        String name,
        String category,
        String brand,
        String model,
        String serialNumber,
        LocalDate purchaseDate,
        BigDecimal purchasePrice,
        LocalDate warrantyStartDate,
        LocalDate warrantyEndDate,
        String warrantyProvider,
        String warrantyStatus, // ACTIVE, EXPIRING_SOON, EXPIRED, UNKNOWN
        Long warrantyDaysRemaining,
        String notes,
        int maintenanceCount,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt
) {}
