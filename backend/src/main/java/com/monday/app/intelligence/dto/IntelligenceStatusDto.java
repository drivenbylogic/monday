package com.monday.app.intelligence.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record IntelligenceStatusDto(
        boolean canGenerate,
        boolean isRunning,
        OffsetDateTime lastGeneratedAt,
        OffsetDateTime nextAllowedAt,
        long cooldownSecondsRemaining,
        int activeNodesCount,
        long totalCachedArticlesCount,
        int retentionDays,
        String lastRunStatus,
        Long lastRunDurationMs,
        Integer lastRunTokensUsed,
        BigDecimal lastRunEstimatedCost
) {}
