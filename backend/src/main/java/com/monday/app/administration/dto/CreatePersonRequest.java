package com.monday.app.administration.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record CreatePersonRequest(
        @NotBlank(message = "Display name is required") String displayName,
        @JsonProperty("relationshipLabel") @JsonAlias({"relationship", "relationship_label"}) String relationshipLabel,
        LocalDate birthday,
        @JsonProperty("contactInformation") @JsonAlias({"contactInfo", "contact_info", "contact_information"}) String contactInformation,
        @JsonProperty("notes") @JsonAlias({"generalNotes", "general_notes"}) String notes
) {
    public String relationship() {
        return relationshipLabel;
    }
}

