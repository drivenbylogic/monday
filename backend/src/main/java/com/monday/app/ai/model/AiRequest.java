package com.monday.app.ai.model;

public record AiRequest(
    AiProvider provider,
    String model,
    String prompt
) {
    public AiRequest {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("Model cannot be null or blank");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be null or blank");
        }
    }
}
