package com.monday.app.administration.dto;

import java.time.LocalDate;

public record HabitPauseRequest(
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {}

