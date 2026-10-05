package com.monday.app.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Standardised error response returned by all API endpoints.
 *
 * <pre>
 * {
 *   "code":      "TASK_NOT_FOUND",
 *   "message":   "Task does not exist",
 *   "details":   {},
 *   "timestamp":  "...",
 *   "path":      "/api/v1/tasks/..."
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String code,
        String message,
        Map<String, Object> details,
        Instant timestamp,
        String path
) {

    public ApiErrorResponse(String code, String message, String path) {
        this(code, message, null, Instant.now(), path);
    }

    public ApiErrorResponse(String code, String message, Map<String, Object> details, String path) {
        this(code, message, details, Instant.now(), path);
    }
}
