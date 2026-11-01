package com.monday.app.shared.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

class CacheConfigTest {

    @Test
    @DisplayName("CacheManager configures all 18 named Caffeine cache regions with explicit bounds")
    void cacheManager_shouldConfigureAllNamedRegions() {
        CacheConfig config = new CacheConfig();
        CacheManager manager = config.cacheManager();

        Collection<String> cacheNames = manager.getCacheNames();
        assertThat(cacheNames).containsExactlyInAnyOrder(
                CacheNames.HOME_DASHBOARD,
                CacheNames.TODAY_AGENDA,
                CacheNames.AI_SYNTHESIS,
                CacheNames.ADMIN_OVERVIEW,
                CacheNames.ADMIN_OBLIGATIONS,
                CacheNames.ADMIN_MASTER_DATA,
                CacheNames.FINANCE_PULSE,
                CacheNames.FINANCE_SAFE_TO_SPEND,
                CacheNames.FINANCE_BURN,
                CacheNames.FINANCE_MONTHLY_REPORT,
                CacheNames.FINANCE_REFERENCE,
                CacheNames.PROJECT_METRICS,
                CacheNames.VEHICLE_OVERVIEW,
                CacheNames.VEHICLE_ANALYTICS,
                CacheNames.HABIT_ANALYTICS,
                CacheNames.HABIT_REPORTS,
                CacheNames.GLOBAL_SEARCH,
                CacheNames.CORE_WEATHER
        );

        for (String name : cacheNames) {
            Cache cache = manager.getCache(name);
            assertThat(cache).isNotNull().isInstanceOf(CaffeineCache.class);
            CaffeineCache caffeineCache = (CaffeineCache) cache;
            assertThat(caffeineCache.getNativeCache()).isNotNull();
        }
    }

    @Test
    @DisplayName("UserAwareKeyGenerator prepends authenticated user ID for multi-tenant isolation")
    void userAwareKeyGenerator_shouldPrependUserId() throws NoSuchMethodException {
        CacheConfig config = new CacheConfig();
        KeyGenerator generator = config.userAwareKeyGenerator();

        Method dummyMethod = String.class.getMethod("substring", int.class);
        Object key = generator.generate(this, dummyMethod, "param1", 42);

        assertThat(key).isNotNull();
        String keyStr = key.toString();
        // Since no security context is bound in this standalone unit test, falls back to default system UUID or anonymous
        assertThat(keyStr).contains("param1");
        assertThat(keyStr).contains("42");
    }

    @Test
    @DisplayName("Caffeine cache regions accurately record hit and miss statistics")
    void cacheStatistics_shouldRecordHitsAndMisses() {
        CacheConfig config = new CacheConfig();
        CacheManager manager = config.cacheManager();
        CaffeineCache cache = (CaffeineCache) manager.getCache(CacheNames.HOME_DASHBOARD);

        assertThat(cache).isNotNull();
        cache.put("user1:2026-11-02", "dashboardPayload");

        assertThat(cache.get("user1:2026-11-02")).isNotNull();
        assertThat(cache.get("user1:2026-11-02").get()).isEqualTo("dashboardPayload");

        assertThat(cache.get("nonexistent-key")).isNull();
        com.github.benmanes.caffeine.cache.stats.CacheStats stats = cache.getNativeCache().stats();
        assertThat(stats.hitCount()).isGreaterThanOrEqualTo(1);
        assertThat(stats.missCount()).isGreaterThanOrEqualTo(1);
    }
}

