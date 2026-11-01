package com.monday.app.shared.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.monday.app.security.SecurityUtils;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Production two-level caching configuration for Monday backend.
 * Uses Caffeine in-memory bounded caches with custom TTL, maximum capacity,
 * and user-aware key generation to guarantee strict tenant isolation.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        List<CaffeineCache> caches = new ArrayList<>();

        // Home dashboard: 3 minutes, 100 entries
        caches.add(buildCache(CacheNames.HOME_DASHBOARD, Duration.ofMinutes(3), 100));

        // Daily agenda: 30 minutes, 100 entries
        caches.add(buildCache(CacheNames.TODAY_AGENDA, Duration.ofMinutes(30), 100));

        // AI synthesis & article summaries: 24 hours, 500 entries
        caches.add(buildCache(CacheNames.AI_SYNTHESIS, Duration.ofHours(24), 500));

        // Administration overview: 5 minutes, 100 entries
        caches.add(buildCache(CacheNames.ADMIN_OVERVIEW, Duration.ofMinutes(5), 100));

        // Administration obligations: 5 minutes, 100 entries
        caches.add(buildCache(CacheNames.ADMIN_OBLIGATIONS, Duration.ofMinutes(5), 100));

        // Administration master data (people, dates, gadgets): 15 minutes, 200 entries
        caches.add(buildCache(CacheNames.ADMIN_MASTER_DATA, Duration.ofMinutes(15), 200));

        // Finance pulse: 3 minutes, 100 entries
        caches.add(buildCache(CacheNames.FINANCE_PULSE, Duration.ofMinutes(3), 100));

        // Finance safe to spend: 3 minutes, 100 entries
        caches.add(buildCache(CacheNames.FINANCE_SAFE_TO_SPEND, Duration.ofMinutes(3), 100));

        // Finance burn trajectory: 5 minutes, 100 entries
        caches.add(buildCache(CacheNames.FINANCE_BURN, Duration.ofMinutes(5), 100));

        // Finance monthly reports: 10 minutes, 100 entries
        caches.add(buildCache(CacheNames.FINANCE_MONTHLY_REPORT, Duration.ofMinutes(10), 100));

        // Finance reference data (categories, accounts, cards): 30 minutes, 200 entries
        caches.add(buildCache(CacheNames.FINANCE_REFERENCE, Duration.ofMinutes(30), 200));

        // Projects metrics & velocity: 5 minutes, 100 entries
        caches.add(buildCache(CacheNames.PROJECT_METRICS, Duration.ofMinutes(5), 100));

        // Vehicle overview: 5 minutes, 100 entries
        caches.add(buildCache(CacheNames.VEHICLE_OVERVIEW, Duration.ofMinutes(5), 100));

        // Vehicle analytics & outlay: 10 minutes, 100 entries
        caches.add(buildCache(CacheNames.VEHICLE_ANALYTICS, Duration.ofMinutes(10), 100));

        // Habit analytics: 10 minutes, 100 entries
        caches.add(buildCache(CacheNames.HABIT_ANALYTICS, Duration.ofMinutes(10), 100));

        // Habit reports & insights: 10 minutes, 100 entries
        caches.add(buildCache(CacheNames.HABIT_REPORTS, Duration.ofMinutes(10), 100));

        // Global search: 2 minutes, 200 entries
        caches.add(buildCache(CacheNames.GLOBAL_SEARCH, Duration.ofMinutes(2), 200));

        // Core weather: 30 minutes, 50 entries
        caches.add(buildCache(CacheNames.CORE_WEATHER, Duration.ofMinutes(30), 50));

        manager.setCaches(caches);
        manager.afterPropertiesSet();
        return manager;
    }

    private CaffeineCache buildCache(String name, Duration ttl, long maximumSize) {
        return new CaffeineCache(
                name,
                Caffeine.newBuilder()
                        .expireAfterWrite(ttl)
                        .maximumSize(maximumSize)
                        .recordStats()
                        .build()
        );
    }

    /**
     * User-aware KeyGenerator that prefixes all keys with the authenticated user ID.
     * Prevents cross-user data leakage even if multiple users share the same backend process.
     */
    @Bean("userAwareKeyGenerator")
    public KeyGenerator userAwareKeyGenerator() {
        return (target, method, params) -> {
            String userId = SecurityUtils.getCurrentUserId()
                    .map(UUID::toString)
                    .orElse("anonymous");
            StringBuilder sb = new StringBuilder();
            sb.append(userId);
            for (Object param : params) {
                sb.append(":");
                sb.append(param != null ? param.toString() : "null");
            }
            return sb.toString();
        };
    }
}
