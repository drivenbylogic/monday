package com.monday.app.agenda.dto;

import java.time.Instant;
import java.util.UUID;

public record ScheduleConflictDto(
        UUID eventId1,
        String title1,
        Instant start1,
        Instant end1,
        UUID eventId2,
        String title2,
        Instant start2,
        Instant end2,
        long overlapMinutes
) {}
