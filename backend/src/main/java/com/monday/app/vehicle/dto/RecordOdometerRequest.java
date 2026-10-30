package com.monday.app.vehicle.dto;

import java.time.LocalDate;

public record RecordOdometerRequest(
        LocalDate readingDate,
        Integer odometerKm,
        String source,
        Boolean isCorrection,
        String notes
) {}
