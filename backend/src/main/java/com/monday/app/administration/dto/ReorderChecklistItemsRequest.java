package com.monday.app.administration.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record ReorderChecklistItemsRequest(
        @NotEmpty(message = "Item IDs order is required") List<UUID> itemIds
) {}
