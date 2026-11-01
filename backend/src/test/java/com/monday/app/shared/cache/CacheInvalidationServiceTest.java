package com.monday.app.shared.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;

import static org.assertj.core.api.Assertions.assertThat;

class CacheInvalidationServiceTest {

    private CacheManager cacheManager;
    private CacheInvalidationService invalidationService;

    @BeforeEach
    void setUp() {
        CacheConfig config = new CacheConfig();
        this.cacheManager = config.cacheManager();
        this.invalidationService = new CacheInvalidationService(cacheManager);
    }

    @Test
    @DisplayName("Immediate eviction removes key from target cache region")
    void evictImmediate_shouldRemoveKey() {
        cacheManager.getCache(CacheNames.HOME_DASHBOARD).put("key1", "val1");
        assertThat(cacheManager.getCache(CacheNames.HOME_DASHBOARD).get("key1")).isNotNull();

        invalidationService.evictImmediate(CacheNames.HOME_DASHBOARD, "key1");
        assertThat(cacheManager.getCache(CacheNames.HOME_DASHBOARD).get("key1")).isNull();
    }

    @Test
    @DisplayName("Clear cache region removes all entries from that region")
    void clearCache_shouldClearAllEntries() {
        cacheManager.getCache(CacheNames.FINANCE_PULSE).put("k1", "v1");
        cacheManager.getCache(CacheNames.FINANCE_PULSE).put("k2", "v2");

        invalidationService.clearCache(CacheNames.FINANCE_PULSE);
        assertThat(cacheManager.getCache(CacheNames.FINANCE_PULSE).get("k1")).isNull();
        assertThat(cacheManager.getCache(CacheNames.FINANCE_PULSE).get("k2")).isNull();
    }

    @Test
    @DisplayName("Non-transactional evictAllAfterCommit immediately clears specified regions")
    void evictAllAfterCommit_nonTransactional_shouldClearRegions() {
        cacheManager.getCache(CacheNames.FINANCE_PULSE).put("k1", "v1");
        cacheManager.getCache(CacheNames.FINANCE_SAFE_TO_SPEND).put("k2", "v2");

        invalidationService.evictAllAfterCommit(CacheNames.FINANCE_PULSE, CacheNames.FINANCE_SAFE_TO_SPEND);

        assertThat(cacheManager.getCache(CacheNames.FINANCE_PULSE).get("k1")).isNull();
        assertThat(cacheManager.getCache(CacheNames.FINANCE_SAFE_TO_SPEND).get("k2")).isNull();
    }
}

