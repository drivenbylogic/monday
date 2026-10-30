package com.monday.app.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RenewDocumentRequest(
        LocalDate newExpiryDate,
        String newValidUntil,
        String newPolicyNumber,
        BigDecimal newPremiumDue,
        BigDecimal newInsuredDeclaredValue,
        String notes
) {}
