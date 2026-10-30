package com.monday.app.intelligence.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DashboardStatsDto(
        int activeNodes,
        long articlesDiscovered,
        long articlesUnique,
        long summariesGenerated,
        long articlesDeferred,
        long articlesFailed,
        OffsetDateTime lastGenerationTime,
        long totalTokensUsed,
        BigDecimal estimatedExpenditureUsd,
        String period,
        List<ProcessingActivityPoint> processingActivity,
        List<AreaDistributionPoint> areaDistribution,
        List<SourcePerformancePoint> sourcePerformance,
        List<AiUsagePoint> aiUsageOverTime,
        ProcessingOutcomesDto processingOutcomes
) {
    public record ProcessingActivityPoint(
            String date,
            int discovered,
            int deduplicated,
            int summarized,
            int failed
    ) {}

    public record AreaDistributionPoint(
            String area,
            String name,
            long count,
            double percentage
    ) {}

    public record SourcePerformancePoint(
            UUID sourceId,
            String sourceName,
            long discovered,
            long summarized,
            double successRate
    ) {}

    public record AiUsagePoint(
            String date,
            String runId,
            int tokens,
            BigDecimal estimatedCost
    ) {}

    public record ProcessingOutcomesDto(
            long summarized,
            long deduplicated,
            long deferred,
            long failed,
            long total
    ) {}
}
