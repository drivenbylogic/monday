package com.monday.app.administration.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PersonDto(
        UUID id,
        String displayName,
        @JsonProperty("relationshipLabel") @JsonAlias({"relationship", "relationship_label"}) String relationshipLabel,
        LocalDate birthday,
        @JsonProperty("contactInformation") @JsonAlias({"contactInfo", "contact_info", "contact_information"}) String contactInformation,
        @JsonProperty("notes") @JsonAlias({"generalNotes", "general_notes"}) String notes,
        int occasionsCount,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt
) {
    @JsonProperty("relationship")
    public String relationship() {
        return relationshipLabel;
    }

    @JsonProperty("contactInfo")
    public String contactInfo() {
        return contactInformation;
    }

    @JsonProperty("generalNotes")
    public String generalNotes() {
        return notes;
    }
}

