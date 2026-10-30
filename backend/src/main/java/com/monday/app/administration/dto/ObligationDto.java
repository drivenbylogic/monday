package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ObligationDto(
        UUID id,
        String name,
        String description,
        String providerName,
        String category,
        String obligationType,
        BigDecimal expectedAmount,
        String currency,
        String billingFrequency,
        String recurrenceRule,
        LocalDate startDate,
        LocalDate nextDueDate,
        LocalDate endDate,
        LocalDate trialEndDate,
        LocalDate cancellationDeadline,
        boolean autoRenew,
        String status,
        long daysRemaining,
        boolean isDueSoon,
        boolean isOverdue,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt
) {}
