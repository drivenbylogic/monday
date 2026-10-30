package com.monday.app.auth.dto;

import java.util.UUID;

/**
 * Response DTO returned after successful authentication.
 */
public record AuthResponse(
        UUID id,
        String username,
        String email,
        String role
) {}
