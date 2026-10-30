package com.monday.app.agenda.dto;

import java.math.BigDecimal;

public record CreditUtilizationSummaryDto(
        BigDecimal totalBalance,
        BigDecimal totalLimit,
        Double overallUtilizationPercent,
        boolean isAvailable
) {}
