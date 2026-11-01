package com.monday.app.administration.dto;

import java.util.List;
import java.util.UUID;

public record UpdateHabitGroupRequest(
        String name,
        String description,
        Integer displayOrder,
        List<UUID> habitIds
) {}

