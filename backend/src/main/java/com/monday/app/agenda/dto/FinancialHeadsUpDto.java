package com.monday.app.agenda.dto;

import java.math.BigDecimal;

public record FinancialHeadsUpDto(
        String title,
        String message,
        BigDecimal impactAmount,
        String urgency // "HIGH", "MEDIUM", "LOW"
) {}
