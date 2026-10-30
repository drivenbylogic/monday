package com.monday.app.vehicle.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateServiceRecordRequest(
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
        List<CreateServiceItemRequest> items
) {}
