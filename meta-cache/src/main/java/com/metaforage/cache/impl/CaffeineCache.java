package com.metaforage.cache.impl;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.metaforage.cache.Cache;
import com.metaforage.cache.mode.CacheConfig;
import com.metaforage.cache.mode.CacheStats;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine本地缓存实现
 */
public class CaffeineCache<K, V> implements Cache<K, V> {

    private final com.github.benmanes.caffeine.cache.Cache<K, V> cache;
    private final CacheStats stats;

    public CaffeineCache(CacheConfig config) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();

        if (config.getMaximumSize() > 0) {
            builder.maximumSize(config.getMaximumSize());
        }

        if (config.getExpireAfterWrite() > 0) {
            builder.expireAfterWrite(config.getExpireAfterWrite(), config.getTimeUnit());
        }

        if (config.getExpireAfterAccess() > 0) {
            builder.expireAfterAccess(config.getExpireAfterAccess(), config.getTimeUnit());
        }

        if (config.isRecordStats()) {
            builder.recordStats();
        }

        this.cache = builder.build();
        this.stats = new CacheStats(0, 0, 0, 0, 0, 0); // Caffeine有内置统计
    }

    @Override
    public V get(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public V get(K key, V defaultValue) {
        V value = cache.getIfPresent(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public void put(K key, V value, long duration, TimeUnit timeUnit) {
        // Caffeine不支持单个条目的过期时间，需要在构建时统一配置
        put(key, value);
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        // Caffeine的put是覆盖式的，需要手动检查
        if (!containsKey(key)) {
            put(key, value);
            return true;
        }
        return false;
    }

    @Override
    public boolean putIfAbsent(K key, V value, long duration, TimeUnit timeUnit) {
        return putIfAbsent(key, value);
    }

    @Override
    public boolean evict(K key) {
        cache.invalidate(key);
        return true;
    }

    @Override
    public void evictAll(Iterable<K> keys) {
        cache.invalidateAll(keys);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public boolean containsKey(K key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    public long size() {
        return cache.estimatedSize();
    }

    @Override
    public Set<K> keys() {
        return cache.asMap().keySet();
    }

    @Override
    public Collection<V> values() {
        return cache.asMap().values();
    }

    @Override
    public boolean expire(K key, long duration, TimeUnit timeUnit) {
        // Caffeine不支持单个条目的过期时间设置
        return containsKey(key);
    }

    @Override
    public long getExpire(K key, TimeUnit timeUnit) {
        // Caffeine不支持获取单个条目的剩余过期时间
        return -1;
    }

    @Override
    public long increment(K key, long delta) {
        // Caffeine不支持原子操作，需要手动实现
        synchronized (this) {
            V current = get(key);
            long newValue;
            if (current instanceof Number) {
                newValue = ((Number) current).longValue() + delta;
            } else {
                newValue = delta;
            }
            put(key, (V) Long.valueOf(newValue));
            return newValue;
        }
    }

    @Override
    public long decrement(K key, long delta) {
        return increment(key, -delta);
    }

    @Override
    public CacheStats getStats() {
        if (cache instanceof com.github.benmanes.caffeine.cache.Cache) {
            com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats =
                    ((com.github.benmanes.caffeine.cache.Cache<K, V>) cache).stats();
            return new CacheStats(
                    caffeineStats.hitCount(),
                    caffeineStats.missCount(),
                    caffeineStats.loadSuccessCount(),
                    caffeineStats.loadFailureCount(),
                    caffeineStats.totalLoadTime(),
                    caffeineStats.evictionCount()
            );
        }
        return stats;
    }
}