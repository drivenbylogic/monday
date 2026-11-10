package com.monday.app.agenda.dto;

import java.time.LocalDate;

/**
 * Request payload for regenerating or explicitly requesting the Beta Daily Brief.
 */
public record GenerateBetaBriefRequest(
        LocalDate date,
        String timezone,
        Boolean force
) {}

