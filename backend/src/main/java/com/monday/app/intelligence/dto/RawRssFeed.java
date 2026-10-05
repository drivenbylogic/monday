package com.monday.app.intelligence.dto;

import java.util.List;

public record RawRssFeed(
        String title,
        String link,
        String description,
        String language,
        List<RawRssItem> items
) {
}
