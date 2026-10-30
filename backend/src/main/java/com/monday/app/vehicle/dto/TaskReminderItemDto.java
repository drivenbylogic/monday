package com.monday.app.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TaskReminderItemDto(
        String id,
        String itemType, // TASK, OBLIGATION, MAINTENANCE, RENEWAL
        String title,
        String detail,
        String status,
        LocalDate dueDate,
        Integer dueKm,
        BigDecimal amount,
        String priority,
        boolean completed
) {}
