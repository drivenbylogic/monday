package com.monday.app.intelligence.dto;

public record UpdateRssNodeRequest(
        String name,
        String feedUrl,
        String siteUrl,
        String areaOfInterest,
        String description,
        Integer articleLimit,
        Boolean isEnabled
) {}
