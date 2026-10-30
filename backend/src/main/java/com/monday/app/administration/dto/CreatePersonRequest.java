package com.monday.app.administration.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreatePersonRequest(
        @NotBlank(message = "Display name is required") String displayName,
        String relationshipLabel,
        LocalDate birthday,
        String contactInformation,
        String notes
) {}
