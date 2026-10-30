package com.monday.app.agenda.dto;

import java.util.UUID;

public record SourceReferenceDto(
        String type, // "TASK", "PROJECT", "MILESTONE", "EVENT", "COMMITMENT"
        UUID id,
        String title
) {}
