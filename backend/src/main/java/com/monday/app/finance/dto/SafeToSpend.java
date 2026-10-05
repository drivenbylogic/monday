package com.monday.app.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Derived safe-to-spend metric for a budget category.
 * Calculated on demand — never persisted.
 */
public record SafeToSpend(
        UUID budgetId,
        UUID categoryId,
        String categoryName,
        BigDecimal budgetAmount,
        BigDecimal spentAmount,
        BigDecimal remainingBudget,
        BigDecimal dailySafeToSpend,
        int remainingDays,
        LocalDate periodStart,
        LocalDate periodEnd
) {}
