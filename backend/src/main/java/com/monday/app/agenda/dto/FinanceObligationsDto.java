package com.monday.app.agenda.dto;

import java.math.BigDecimal;
import java.util.List;

public record FinanceObligationsDto(
        List<CommitmentSummaryDto> dueToday,
        List<CommitmentSummaryDto> dueSoon, // e.g. within next 7 days
        List<CommitmentSummaryDto> overdue,
        BigDecimal totalDueSoonAmount,
        List<CreditCardSummaryDto> creditCards,
        CreditUtilizationSummaryDto overallCreditUtilization,
        List<InvestmentSipSummaryDto> upcomingSips
) {}
