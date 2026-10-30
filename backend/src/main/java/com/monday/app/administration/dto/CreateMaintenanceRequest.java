package com.monday.app.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateMaintenanceRequest(
        UUID gadgetId,
        @NotBlank(message = "Title is required") String title,
        String description,
        String maintenanceType,
        @NotNull(message = "Due date is required") LocalDate dueDate,
        String recurrenceRule,
        BigDecimal cost,
        String serviceProvider,
        String serviceNotes
) {}
