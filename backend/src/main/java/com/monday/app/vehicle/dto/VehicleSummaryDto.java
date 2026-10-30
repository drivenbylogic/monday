package com.monday.app.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VehicleSummaryDto(
        UUID id,
        String name,
        String edition,
        String variant,
        String type,
        String registrationNumber,
        Boolean isPrimary,
        int currentKm,
        LocalDate purchaseDate,
        BigDecimal purchaseAmount,
        String status,
        String heroImageUrl,
        String vin,
        String emissionStandard,
        String registeredOwner,
        String registeringAuthority
) {}
