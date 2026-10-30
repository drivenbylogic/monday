package com.monday.app.vehicle.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MaintenanceScheduleDto(
        UUID id,
        UUID vehicleId,
        String itemName,
        Integer intervalMonths,
        Integer intervalKm,
        LocalDate lastPerformedAt,
        Integer lastPerformedKm,
        LocalDate nextDueAt,
        Integer nextDueKm,
        String status, // OK, DUE_SOON, OVERDUE
        boolean isOverdue,
        boolean isDueSoon,
        Integer kmRemaining,
        Long daysRemaining,
        Instant createdAt,
        Instant updatedAt
) {}
