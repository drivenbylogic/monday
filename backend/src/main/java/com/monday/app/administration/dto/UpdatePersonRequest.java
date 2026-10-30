package com.monday.app.administration.dto;

import java.time.LocalDate;

public record UpdatePersonRequest(
        String displayName,
        String relationshipLabel,
        LocalDate birthday,
        String contactInformation,
        String notes
) {}
