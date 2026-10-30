package com.monday.app.agenda.dto;

import java.math.BigDecimal;
import java.util.List;

public record BudgetSpendingDto(
        BigDecimal totalBudgeted,
        BigDecimal totalSpent,
        BigDecimal remainingBudget,
        Double consumptionPercent,
        List<CategorySpendingSummaryDto> categories,
        List<RecentTransactionSummaryDto> recentSpending
) {}
