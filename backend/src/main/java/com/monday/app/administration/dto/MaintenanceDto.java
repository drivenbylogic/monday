package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MaintenanceDto(
        UUID id,
        UUID gadgetId,
        String gadgetName,
        String title,
        String description,
        String maintenanceType,
        LocalDate dueDate,
        String recurrenceRule,
        LocalDate lastPerformedAt,
        BigDecimal cost,
        String status, // UPCOMING, DUE, COMPLETED, CANCELLED
        String serviceProvider,
        String serviceNotes,
        long daysRemaining,
        boolean isOverdue,
        Instant createdAt,
        Instant updatedAt
) {}
