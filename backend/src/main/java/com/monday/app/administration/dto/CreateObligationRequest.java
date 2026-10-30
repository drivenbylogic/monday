package com.monday.app.administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateObligationRequest(
        @NotBlank(message = "Name is required") String name,
        String description,
        String providerName,
        String category,
        String obligationType, // BILL, SUBSCRIPTION, MEMBERSHIP, OTHER
        BigDecimal expectedAmount,
        String currency,
        String billingFrequency, // WEEKLY, MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY, CUSTOM
        String recurrenceRule,
        LocalDate startDate,
        LocalDate nextDueDate,
        LocalDate endDate,
        LocalDate trialEndDate,
        LocalDate cancellationDeadline,
        Boolean autoRenew,
        String status
) {}
