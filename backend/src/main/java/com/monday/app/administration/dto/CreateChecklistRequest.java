package com.monday.app.administration.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateChecklistRequest(
        @NotBlank(message = "Title is required") String title,
        String description,
        String administrationRecordType,
        UUID administrationRecordId,
        String parentRecordType,
        UUID parentRecordId
) {
    public CreateChecklistRequest(String title, String description, String administrationRecordType, UUID administrationRecordId) {
        this(title, description, administrationRecordType, administrationRecordId, administrationRecordType, administrationRecordId);
    }

    public String getRecordType() {
        return administrationRecordType != null ? administrationRecordType : parentRecordType;
    }

    public UUID getRecordId() {
        return administrationRecordId != null ? administrationRecordId : parentRecordId;
    }
}
