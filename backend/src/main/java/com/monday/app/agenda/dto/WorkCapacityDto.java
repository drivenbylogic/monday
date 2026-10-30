package com.monday.app.agenda.dto;

public record WorkCapacityDto(
        int totalWorkDayMinutes, // 480 mins (8h)
        int scheduledEventMinutes,
        int availableWorkMinutes, // max(0, 480 - scheduledEventMinutes)
        int estimatedTaskWorkloadMinutes, // sum of due today + high priority task estimates
        boolean isOverloaded
) {}
