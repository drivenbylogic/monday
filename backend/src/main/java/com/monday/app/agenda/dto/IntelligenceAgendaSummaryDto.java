package com.monday.app.agenda.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Programmatic dashboard and agenda context for the Intelligence module.
 */
public record IntelligenceAgendaSummaryDto(
        long articlesDiscoveredToday,
        long summariesGeneratedToday,
        int activeFeedNodes,
        List<IntelligenceArticleSummaryDto> topDailyArticles
) {
    public record IntelligenceArticleSummaryDto(
            UUID id,
            String title,
            String sourceName,
            String areaOfInterest,
            OffsetDateTime publishedAt,
            String summarySnippet,
            boolean isRead
    ) {}
}
