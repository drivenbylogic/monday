package com.monday.app.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record CreateHabitRequest(
        @NotBlank(message = "Habit name is required")
        String name,
        String description,
        @NotBlank(message = "Category is required")
        String category,
        @NotBlank(message = "Frequency type is required")
        String frequencyType,
        String scheduleConfig,
        BigDecimal targetValue,
        BigDecimal minimumTargetValue,
        String targetUnit,
        String priority,
        @NotNull(message = "Start date is required")
        LocalDate startDate,
        LocalDate endDate,
        UUID groupId,
        Integer displayOrder,
        Integer estimatedDuration,
        List<LocalTime> reminderTimes
) {}

