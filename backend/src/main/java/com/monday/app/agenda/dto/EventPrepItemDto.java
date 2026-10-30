package com.monday.app.agenda.dto;

import java.time.LocalDate;
import java.util.UUID;

public record EventPrepItemDto(
        UUID id,
        UUID eventId,
        String eventTitle,
        String text,
        boolean completed,
        LocalDate due
) {}
