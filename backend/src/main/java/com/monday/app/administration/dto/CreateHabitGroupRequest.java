package com.monday.app.administration.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record CreateHabitGroupRequest(
        @NotBlank(message = "Group name is required")
        String name,
        String description,
        Integer displayOrder,
        List<UUID> habitIds
) {}

