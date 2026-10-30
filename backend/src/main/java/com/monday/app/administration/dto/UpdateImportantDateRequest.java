package com.monday.app.administration.dto;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateImportantDateRequest(
        String title,
        String description,
        LocalDate date,
        String dateType,
        UUID personId,
        String recurrenceType,
        String recurrenceRule,
        Integer reminderLeadDays
) {}
