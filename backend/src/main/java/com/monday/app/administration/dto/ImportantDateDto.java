package com.monday.app.administration.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ImportantDateDto(
        UUID id,
        String title,
        String description,
        LocalDate date,
        String dateType, // BIRTHDAY, ANNIVERSARY, PERSONAL_MILESTONE, FAMILY_EVENT, OTHER
        UUID personId,
        String personName,
        String recurrenceType, // ANNUAL, ONCE, MONTHLY, CUSTOM
        String recurrenceRule,
        int reminderLeadDays,
        LocalDate nextOccurrence,
        long daysRemaining,
        boolean isUpcomingSoon,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt
) {}
