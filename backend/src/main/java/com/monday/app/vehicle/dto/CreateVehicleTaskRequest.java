package com.monday.app.vehicle.dto;

public record CreateVehicleTaskRequest(
        String title,
        String metaText,
        String priority,
        String linkedModule
) {}
