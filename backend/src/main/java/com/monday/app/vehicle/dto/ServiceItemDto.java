package com.monday.app.vehicle.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ServiceItemDto(
        UUID id,
        UUID serviceRecordId,
        String itemName,
        String category,
        String brandSpec,
        BigDecimal quantity,
        String unit,
        BigDecimal itemCost,
        Integer replacementIntervalMonths,
        Integer replacementIntervalKm,
        String notes,
        Instant createdAt
) {}
