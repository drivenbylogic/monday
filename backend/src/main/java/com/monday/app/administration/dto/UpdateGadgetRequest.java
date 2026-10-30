package com.monday.app.administration.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateGadgetRequest(
        String name,
        String category,
        String brand,
        String model,
        String serialNumber,
        LocalDate purchaseDate,
        BigDecimal purchasePrice,
        LocalDate warrantyStartDate,
        LocalDate warrantyEndDate,
        String warrantyProvider,
        String notes
) {}
