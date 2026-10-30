package com.monday.app.agenda.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CategorySpendingSummaryDto(
        UUID categoryId,
        String categoryName,
        BigDecimal budgetedAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        Double consumptionPercent,
        boolean isOverLimit,
        boolean isApproachingLimit // >= 80%
) {}
