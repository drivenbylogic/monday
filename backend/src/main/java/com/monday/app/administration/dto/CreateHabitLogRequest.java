package com.monday.app.administration.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateHabitLogRequest(
        @NotNull(message = "Habit ID is required")
        UUID habitId,
        @NotNull(message = "Entry date is required")
        LocalDate entryDate,
        BigDecimal actualValue,
        String status, // COMPLETED, PARTIAL
        String notes
) {}

