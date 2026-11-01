package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record HabitLogDto(
        UUID id,
        UUID habitId,
        String habitName,
        LocalDate entryDate,
        BigDecimal actualValue,
        String status, // COMPLETED, PARTIAL
        String notes,
        Instant createdAt
) {}

