package com.monday.app.intelligence.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RssNodeDto(
        UUID id,
        String name,
        String feedUrl,
        String siteUrl,
        String areaOfInterest,
        String description,
        Boolean isEnabled,
        Integer articleLimit,
        Double sourceQuality,
        OffsetDateTime lastFetchedAt,
        OffsetDateTime lastSuccessAt,
        OffsetDateTime lastFailureAt,
        String lastFetchStatus,
        String lastFailureMessage,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        Long articleCount
) {}
