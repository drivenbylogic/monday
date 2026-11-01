package com.monday.app.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    // Default system user for unauthenticated requests or tests
    public static final UUID DEFAULT_SYSTEM_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private SecurityUtils() {}

    /**
     * Resolves the current authenticated user's UUID.
     * Falls back to DEFAULT_SYSTEM_USER_ID if no authenticated session is present.
     */
    public static Optional<UUID> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof MondayUserDetails details) {
            return Optional.ofNullable(details.getId());
        }
        return Optional.of(DEFAULT_SYSTEM_USER_ID);
    }

    public static UUID getCurrentUserIdOrThrow() {
        return getCurrentUserId().orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }
}

