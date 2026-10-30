package com.monday.app.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VehicleObligationDto(
        UUID id,
        UUID vehicleId,
        String obligationType,
        LocalDate dueDate,
        BigDecimal amount,
        String status,
        String notes,
        Long daysRemaining,
        boolean isDueSoon,
        boolean isOverdue
) {}
