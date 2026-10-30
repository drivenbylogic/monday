package com.monday.app.agenda.dto;

public record DeepWorkSummaryDto(
        int totalMinutesLast7Days,
        double totalHoursLast7Days,
        double targetHours,
        int sessionsCountLast7Days,
        int averageFocusScore
) {}
