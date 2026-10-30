package com.monday.app.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VehicleTimelineItemDto(
        String id,
        String eventType, // SERVICE, FUEL, ODOMETER, DOCUMENT, INSURANCE, TASK, OBLIGATION
        String title,
        String description,
        LocalDate eventDate,
        Integer odometerKm,
        BigDecimal cost,
        String recordId,
        String badge,
        String icon
) {}
