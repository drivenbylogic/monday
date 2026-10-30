package com.monday.app.administration.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateChecklistItemRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        Integer position
) {}
