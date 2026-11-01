package com.monday.app.administration.dto;

import java.math.BigDecimal;

public record PartialOccurrenceRequest(
        BigDecimal actualValue,
        String notes
) {}

