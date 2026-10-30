package com.monday.app.vehicle.dto;

import java.math.BigDecimal;

public record CreateServiceItemRequest(
        String itemName,
        String category,
        String brandSpec,
        BigDecimal quantity,
        String unit,
        BigDecimal itemCost,
        Integer replacementIntervalMonths,
        Integer replacementIntervalKm,
        String notes
) {}
