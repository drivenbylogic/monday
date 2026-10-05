package com.monday.app.auth.dto;

/**
 * Response DTO returned after successful authentication.
 */
public record AuthResponse(
        String token,
        String username,
        String role
) {}
