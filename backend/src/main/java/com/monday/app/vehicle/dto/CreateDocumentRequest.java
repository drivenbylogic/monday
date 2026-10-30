package com.monday.app.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateDocumentRequest(
        String docType,
        String title,
        String identifier,
        String issuer,
        String validUntil,
        LocalDate issueDate,
        LocalDate validFrom,
        LocalDate expiryDate,
        String verificationSource,
        Boolean isVerified,
        String status,
        String notes,
        String metadata,
        // Insurance-specific fields when docType is INSURANCE
        String policyName,
        String underwriter,
        String policyNumber,
        String coverageScope,
        BigDecimal insuredDeclaredValue,
        BigDecimal premiumDue,
        LocalDate insuranceDueDate
) {}
