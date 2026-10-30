package com.monday.app.administration.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.UUID;

public record CreateImportantDateRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        LocalDate date,
        LocalDate eventDate,
        String dateType,
        UUID personId,
        String recurrenceType,
        String recurrenceRule,
        Integer reminderLeadDays
) {
    public LocalDate resolveDate() {
        if (date != null) return date;
        if (eventDate != null) return eventDate;
        return LocalDate.now();
    }
}
