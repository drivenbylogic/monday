package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateObligationRequest(
        String name,
        String description,
        String providerName,
        String category,
        String obligationType,
        BigDecimal expectedAmount,
        String currency,
        String billingFrequency,
        String recurrenceRule,
        LocalDate startDate,
        LocalDate nextDueDate,
        LocalDate endDate,
        LocalDate trialEndDate,
        LocalDate cancellationDeadline,
        Boolean autoRenew,
        String status
) {}
