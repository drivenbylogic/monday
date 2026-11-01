package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HabitOccurrenceDto(
        UUID id,
        UUID habitId,
        String habitName,
        String category,
        LocalDate scheduledDate,
        BigDecimal targetValueSnapshot,
        BigDecimal minimumTargetSnapshot,
        String targetUnitSnapshot,
        String status, // PENDING, COMPLETED, PARTIAL, MISSED, SKIPPED
        BigDecimal actualValue,
        Instant completedAt,
        String notes,
        String skipReason,
        String priority,
        Integer estimatedDuration,
        UUID groupId,
        String groupName
) {}

