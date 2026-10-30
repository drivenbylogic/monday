package com.monday.app.agenda.dto;

public record AgendaAlertDto(
        String type, // "DEADLINE", "OVERLOAD", "FINANCIAL", "CONFLICT"
        String title,
        String message,
        String severity // "DANGER", "WARNING", "INFO"
) {}
