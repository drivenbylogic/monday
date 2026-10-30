package com.monday.app.vehicle.dto;

import java.time.LocalDate;

public record UpdateMaintenanceScheduleRequest(
        String itemName,
        Integer intervalMonths,
        Integer intervalKm,
        LocalDate lastPerformedAt,
        Integer lastPerformedKm,
        LocalDate nextDueAt,
        Integer nextDueKm,
        String status
) {}
