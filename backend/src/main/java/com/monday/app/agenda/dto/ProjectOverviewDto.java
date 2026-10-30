package com.monday.app.agenda.dto;

import java.util.List;

public record ProjectOverviewDto(
        int activeProjectCount,
        List<ProjectProgressSummaryDto> activeProjects,
        List<MilestoneSummaryDto> upcomingMilestones,
        DeepWorkSummaryDto recentDeepWork
) {}
