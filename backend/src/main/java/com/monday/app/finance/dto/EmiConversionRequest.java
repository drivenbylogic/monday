package com.monday.app.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EmiConversionRequest(
        @NotNull(message = "Tenure in months is required")
        @Min(value = 2, message = "Tenure must be at least 2 months")
        @Max(value = 60, message = "Tenure cannot exceed 60 months")
        Integer tenureMonths,

        @DecimalMin(value = "0.0", message = "Interest rate cannot be negative")
        BigDecimal annualInterestRate,

        @DecimalMin(value = "0.0", message = "Processing fee cannot be negative")
        BigDecimal processingFee,

        LocalDate firstInstallmentDate
) {}

