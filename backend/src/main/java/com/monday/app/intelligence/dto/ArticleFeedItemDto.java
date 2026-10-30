package com.monday.app.intelligence.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ArticleFeedItemDto(
        UUID id,
        String title,
        String summary,
        String originalUrl,
        String canonicalUrl,
        String author,
        String sourceName,
        UUID feedId,
        String areaOfInterest,
        OffsetDateTime publishedAt,
        OffsetDateTime firstSeenAt,
        OffsetDateTime summaryGeneratedAt,
        String model,
        Integer wordCount,
        Integer tokensUsed,
        BigDecimal estimatedCost,
        Boolean bookmarked,
        Boolean isRead,
        String processingStatus
) {}
