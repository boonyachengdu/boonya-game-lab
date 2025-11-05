package com.metaforage.cache;

import com.metaforage.cache.component.CacheManager;
import com.metaforage.cache.mode.CacheConfig;
import com.metaforage.cache.mode.CacheMode;

public abstract class AbstractCache<K, V> {
    private Cache<K, V> cache;

    public void put(K key, V value) {
        cache.put(key, value);
    }

    public V get(K key) {
        return cache.get(key);
    }

    public void evict(K key) {
        cache.evict(key);
    }

    public abstract Cache<K, V> getCache();

    public Cache<K, V> setLocalOrDistributedCache(String cacheName, CacheConfig cacheConfig, CacheManager cacheManager) {
        if (cache != null) {
            return cache;
        }
        if (cacheConfig.getCacheMode().equals(CacheMode.LOCAL)) {
            cache = cacheManager.getLocalCache(cacheName, cacheConfig);
        } else {
            cache = cacheManager.getDistributedCache(cacheName);
        }
        return cache;
    }
}
