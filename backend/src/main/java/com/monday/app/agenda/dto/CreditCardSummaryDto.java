package com.monday.app.agenda.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreditCardSummaryDto(
        UUID id,
        String cardId,
        String cardName,
        String lastFour,
        String network,
        BigDecimal creditLimit,
        BigDecimal currentBalance,
        LocalDate statementClosingDate,
        LocalDate paymentDueDate,
        BigDecimal minAmountDue,
        boolean autoPayEnabled,
        Double utilizationPercent // null if creditLimit <= 0
) {}
