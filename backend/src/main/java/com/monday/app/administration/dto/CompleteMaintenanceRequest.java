package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CompleteMaintenanceRequest(
        LocalDate performedAt,
        BigDecimal cost,
        String serviceProvider,
        String serviceNotes,
        boolean scheduleNextOccurrence
) {}
