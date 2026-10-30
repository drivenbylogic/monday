package com.monday.app.agenda.dto;

import java.time.LocalDate;

public record AgendaFeedbackRequest(
        LocalDate date,
        String recommendationId,
        String action // "ACCEPTED", "DISMISSED", "DEFERRED"
) {}
