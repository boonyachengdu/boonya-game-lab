package com.metaforage.cache.component;

import com.metaforage.cache.Cache;
import com.metaforage.cache.mode.CacheConfig;
import com.metaforage.cache.impl.CaffeineCache;
import com.metaforage.cache.impl.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存管理器
 */
@Component
public class CacheManager {

    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;

    public CacheManager(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取或创建本地缓存
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getLocalCache(String name, CacheConfig config) {
        return (Cache<K, V>) caches.computeIfAbsent(
                "local:" + name,
                k -> new CaffeineCache<K, V>(config)
        );
    }

    /**
     * 获取或创建分布式缓存
     */
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getDistributedCache(String name) {
        return (Cache<K, V>) caches.computeIfAbsent(
                "distributed:" + name,
                k -> new RedisCache<K, V>((RedisTemplate<K, V>) redisTemplate, name)
        );
    }

    /**
     * 获取所有缓存名称
     */
    public Set<String> getCacheNames() {
        return caches.keySet();
    }

    /**
     * 清空指定缓存
     */
    public void clearCache(String name) {
        Cache<?, ?> cache = caches.get(name);
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * 销毁缓存
     */
    public void destroyCache(String name) {
        Cache<?, ?> cache = caches.remove(name);
        if (cache != null) {
            cache.clear();
        }
    }
}
