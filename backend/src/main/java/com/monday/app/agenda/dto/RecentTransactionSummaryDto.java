package com.monday.app.agenda.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RecentTransactionSummaryDto(
        UUID id,
        String description,
        BigDecimal amount,
        Instant transactionAt,
        String categoryName,
        String transactionType,
        String paymentMethod
) {}
