package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HabitDto(
        UUID id,
        UUID userId,
        UUID groupId,
        String groupName,
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
        String status,
        int displayOrder,
        Integer estimatedDuration,
        List<HabitReminderDto> reminders,
        int currentStreak,
        int longestStreak,
        double completionRate30Days,
        Instant createdAt,
        Instant updatedAt
) {}

