package com.monday.app.agenda.dto;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Top-level response for the Beta Daily Brief Agent Agenda.
 * Encapsulates the synthesized AI briefing along with the grounded input snapshot,
 * execution mode, and metadata.
 */
public record BetaDailyBriefResponse(
        LocalDate date,
        String status, // GENERATED, FALLBACK, STALE, FAILED
        Instant generatedAt,
        String generationMode, // AI, DETERMINISTIC
        BetaDailyBriefDto briefing,
        DailyBriefSnapshotDto snapshot,
        boolean isStale,
        String staleReason
) {}

