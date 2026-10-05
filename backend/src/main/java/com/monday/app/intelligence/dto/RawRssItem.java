package com.monday.app.intelligence.dto;

import java.util.List;

public record RawRssItem(
        String title,
        String link,
        String description,
        String author,
        List<String> categories,
        String guid,
        String pubDate
) {
}
