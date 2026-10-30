package com.monday.app.vehicle.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record VehicleDocumentDto(
        UUID id,
        UUID vehicleId,
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
        String coveredComponents,
        String notes,
        String metadata,
        Long daysRemaining,
        boolean isExpiringSoon,
        boolean isExpired,
        boolean isIndefinite,
        String effectiveStatus, // VALID, EXPIRING_SOON, EXPIRED, INDEFINITE, ARCHIVED
        // Optional linked insurance fields
        UUID insuranceId,
        String policyName,
        String underwriter,
        String policyNumber,
        String coverageScope,
        BigDecimal insuredDeclaredValue,
        BigDecimal premiumDue,
        LocalDate insuranceDueDate,
        Boolean isInsuranceRenewed,
        Instant createdAt,
        Instant updatedAt
) {}
