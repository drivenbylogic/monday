package com.monday.app.vehicle.dto;

import java.util.List;

public record VehicleOverviewDto(
        VehicleSummaryDto vehicle,
        MetricCardDto metrics,
        List<NeedsAttentionItemDto> needsAttention,
        List<TaskReminderItemDto> tasksAndReminders,
        List<VehicleTimelineItemDto> recentTimeline
) {}
