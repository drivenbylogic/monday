package com.monday.app.administration.dto;

import java.time.Instant;

public record UpdateAppointmentRequest(
        String title,
        String description,
        String eventType,
        Instant startAt,
        Instant endAt,
        Boolean allDay,
        String location,
        String status,
        Boolean requiresPrep,
        String prepTitle,
        String prepDueText
) {}
