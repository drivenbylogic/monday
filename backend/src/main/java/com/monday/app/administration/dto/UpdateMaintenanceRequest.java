package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateMaintenanceRequest(
        String title,
        String description,
        String maintenanceType,
        LocalDate dueDate,
        String recurrenceRule,
        LocalDate lastPerformedAt,
        BigDecimal cost,
        String status,
        String serviceProvider,
        String serviceNotes
) {}
