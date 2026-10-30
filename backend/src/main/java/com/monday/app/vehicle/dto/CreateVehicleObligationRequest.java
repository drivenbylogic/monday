package com.monday.app.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateVehicleObligationRequest(
        String obligationType,
        LocalDate dueDate,
        BigDecimal amount,
        String status,
        String notes
) {}
