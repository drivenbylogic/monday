package com.monday.app.shared.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Transaction-aware Cache Invalidation Service.
 * Guarantees that cache evictions execute strictly AFTER the database transaction commits,
 * ensuring rollbacks do not evict valid cached state and dirty reads are prevented.
 */
@Service
public class CacheInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationService.class);

    private final CacheManager cacheManager;

    public CacheInvalidationService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Evicts a single key from a named cache after transaction commit (or immediately if non-transactional).
     */
    public void evictAfterCommit(String cacheName, Object key) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doEvict(cacheName, key);
                }
            });
        } else {
            doEvict(cacheName, key);
        }
    }

    /**
     * Clears one or more entire cache regions after transaction commit.
     */
    public void evictAllAfterCommit(String... cacheNames) {
        if (cacheNames == null || cacheNames.length == 0) return;

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (String name : cacheNames) {
                        doClear(name);
                    }
                }
            });
        } else {
            for (String name : cacheNames) {
                doClear(name);
            }
        }
    }

    /**
     * Immediately evicts a single key from a named cache.
     */
    public void evictImmediate(String cacheName, Object key) {
        doEvict(cacheName, key);
    }

    /**
     * Immediately clears an entire named cache.
     */
    public void clearCache(String cacheName) {
        doClear(cacheName);
    }

    private void doEvict(String cacheName, Object key) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(key);
                log.debug("Evicted cache key [{}] from region [{}]", key, cacheName);
            }
        } catch (Exception e) {
            log.warn("Failed to evict cache key [{}] from [{}]: {}", key, cacheName, e.getMessage());
        }
    }

    private void doClear(String cacheName) {
        try {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.debug("Cleared entire cache region [{}]", cacheName);
            }
        } catch (Exception e) {
            log.warn("Failed to clear cache region [{}]: {}", cacheName, e.getMessage());
        }
    }
}

