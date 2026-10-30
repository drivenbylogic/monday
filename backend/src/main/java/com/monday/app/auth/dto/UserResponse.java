package com.monday.app.auth.dto;

import java.util.UUID;

/**
 * Response DTO returned for authenticated user information.
 */
public record UserResponse(
        UUID id,
        String username,
        String email,
        String role
) {}

