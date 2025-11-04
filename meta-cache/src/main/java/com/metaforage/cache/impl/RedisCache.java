package com.metaforage.cache.impl;

import com.metaforage.cache.Cache;
import com.metaforage.cache.mode.CacheStats;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式缓存实现
 */
public class RedisCache<K, V> implements Cache<K, V> {

    private final RedisTemplate<K, V> redisTemplate;
    private final String cacheName;
    private final CacheStats stats;

    public RedisCache(RedisTemplate<K, V> redisTemplate, String cacheName) {
        this.redisTemplate = redisTemplate;
        this.cacheName = cacheName;
        this.stats = new CacheStats(0, 0, 0, 0, 0, 0); // Redis需要自定义统计
    }

    private K buildKey(K key) {
        // 为key添加缓存名前缀
        return (K) (cacheName + ":" + key.toString());
    }

    @Override
    public V get(K key) {
        K fullKey = buildKey(key);
        return redisTemplate.opsForValue().get(fullKey);
    }

    @Override
    public V get(K key, V defaultValue) {
        V value = get(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public void put(K key, V value) {
        K fullKey = buildKey(key);
        redisTemplate.opsForValue().set(fullKey, value);
    }

    @Override
    public void put(K key, V value, long duration, TimeUnit timeUnit) {
        K fullKey = buildKey(key);
        redisTemplate.opsForValue().set(fullKey, value, duration, timeUnit);
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        K fullKey = buildKey(key);
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(fullKey, value));
    }

    @Override
    public boolean putIfAbsent(K key, V value, long duration, TimeUnit timeUnit) {
        K fullKey = buildKey(key);
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(fullKey, value, duration, timeUnit));
    }

    @Override
    public boolean evict(K key) {
        K fullKey = buildKey(key);
        return Boolean.TRUE.equals(redisTemplate.delete(fullKey));
    }

    @Override
    public void evictAll(Iterable<K> keys) {
        List<K> fullKeys = new ArrayList<>();
        for (K key : keys) {
            fullKeys.add(buildKey(key));
        }
        redisTemplate.delete(fullKeys);
    }

    @Override
    public void clear() {
        // 删除该缓存名前缀的所有key
        Set<K> keys = redisTemplate.keys((K) (cacheName + ":*"));
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Override
    public boolean containsKey(K key) {
        K fullKey = buildKey(key);
        return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
    }

    @Override
    public long size() {
        Set<K> keys = redisTemplate.keys((K) (cacheName + ":*"));
        return keys != null ? keys.size() : 0;
    }

    @Override
    public Set<K> keys() {
        Set<K> keys = redisTemplate.keys((K) (cacheName + ":*"));
        // 移除前缀返回原始key
        Set<K> result = new HashSet<>();
        if (keys != null) {
            for (K key : keys) {
                String keyStr = key.toString();
                result.add((K) keyStr.substring(cacheName.length() + 1));
            }
        }
        return result;
    }

    @Override
    public Collection<V> values() {
        Set<K> keys = redisTemplate.keys((K) (cacheName + ":*"));
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        return redisTemplate.opsForValue().multiGet(keys);
    }

    @Override
    public boolean expire(K key, long duration, TimeUnit timeUnit) {
        K fullKey = buildKey(key);
        return Boolean.TRUE.equals(redisTemplate.expire(fullKey, duration, timeUnit));
    }

    @Override
    public long getExpire(K key, TimeUnit timeUnit) {
        K fullKey = buildKey(key);
        return redisTemplate.getExpire(fullKey, timeUnit);
    }

    @Override
    public long increment(K key, long delta) {
        K fullKey = buildKey(key);
        return redisTemplate.opsForValue().increment(fullKey, delta);
    }

    @Override
    public long decrement(K key, long delta) {
        K fullKey = buildKey(key);
        return redisTemplate.opsForValue().decrement(fullKey, delta);
    }

    @Override
    public CacheStats getStats() {
        // Redis需要自定义统计逻辑
        return stats;
    }
}
