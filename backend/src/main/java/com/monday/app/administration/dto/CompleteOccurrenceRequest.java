package com.monday.app.administration.dto;

import java.math.BigDecimal;

public record CompleteOccurrenceRequest(
        BigDecimal actualValue,
        String notes
) {}

