package com.monday.app.intelligence.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAreaRequest(
        @NotBlank(message = "Area name is required")
        String name,
        String code,
        String description
) {}
