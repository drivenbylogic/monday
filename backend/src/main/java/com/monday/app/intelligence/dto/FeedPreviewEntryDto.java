package com.monday.app.intelligence.dto;

public record FeedPreviewEntryDto(
        String title,
        String link,
        String publishedAt,
        String description,
        String author
) {}
