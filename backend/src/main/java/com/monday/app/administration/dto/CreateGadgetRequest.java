package com.monday.app.administration.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateGadgetRequest(
        @NotBlank(message = "Gadget name is required") String name,
        @NotBlank(message = "Category is required") String category,
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
