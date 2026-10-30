package com.monday.app.agenda.dto;

import java.time.Instant;
import java.util.List;

public record AgendaRecommendationDto(
        String id,
        String title,
        String description,
        String priority, // "HIGH", "MEDIUM", "LOW"
        String reason,
        String nextAction,
        List<SourceReferenceDto> sourceReferences,
        Integer suggestedDurationMinutes,
        Instant suggestedStartAt,
        Instant suggestedEndAt,
        Double confidence,
        String feedbackStatus // null, "ACCEPTED", "DISMISSED", "DEFERRED"
) {}
