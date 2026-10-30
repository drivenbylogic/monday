package com.monday.app.project.dto;

public record DeepWorkSessionResponse(
        String id,
        Double loggedHours,
        Double weeklyTotalHours,
        Double targetWeeklyHours,
        String status
) {}
