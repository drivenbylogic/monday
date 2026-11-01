package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record UpdateHabitRequest(
        String name,
        String description,
        String category,
        String frequencyType,
        String scheduleConfig,
        BigDecimal targetValue,
        BigDecimal minimumTargetValue,
        String targetUnit,
        String priority,
        LocalDate startDate,
        LocalDate endDate,
        UUID groupId,
        Integer displayOrder,
        Integer estimatedDuration,
        List<LocalTime> reminderTimes
) {}

