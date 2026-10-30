package com.monday.app.agenda.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record InvestmentSipSummaryDto(
        UUID id,
        String name,
        String instrument,
        String assetClass,
        BigDecimal sipAmount,
        Integer sipDay, // Day of month (1-31)
        String status,
        String note // Note that this is a scheduled contribution indication
) {}
