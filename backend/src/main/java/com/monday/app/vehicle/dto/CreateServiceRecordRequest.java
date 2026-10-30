package com.monday.app.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateServiceRecordRequest(
        LocalDate serviceDate,
        Integer kmAtService,
        String serviceType,
        String description,
        BigDecimal cost,
        BigDecimal partsCost,
        BigDecimal laborCost,
        String performedBy,
        String notes,
        Boolean verified,
        Boolean isUpcoming,
        Boolean syncToFinance,
        UUID linkedScheduleId,
        List<CreateServiceItemRequest> items
) {}
