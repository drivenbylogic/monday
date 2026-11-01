package com.monday.app.administration.dto;

import java.math.BigDecimal;

public record UpdateHabitLogRequest(
        BigDecimal actualValue,
        String status,
        String notes
) {}

