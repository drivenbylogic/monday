package com.monday.app.intelligence.dto;

import java.util.List;

public record FeedPreviewResponse(
        boolean reachable,
        String title,
        String description,
        String link,
        int entryCount,
        List<FeedPreviewEntryDto> entries,
        String errorMessage
) {}
