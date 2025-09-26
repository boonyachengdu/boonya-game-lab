package com.boonya.game.cache;

import lombok.Data;
import org.springframework.cache.CacheManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * 多级缓存管理器
 */
@Data
public abstract class AbstractMultiLevelCache<T, ID> {

    /**
     * L1: 本地缓存
     */
    private CacheManager localCache;

    /**
     * L2: Redis缓存
     */
    private RedisTemplate<String, Object> redisCache;

    public AbstractMultiLevelCache(CacheManager localCache, RedisTemplate<String, Object> redisCache) {
        this.localCache = localCache;
        this.redisCache = redisCache;
    }

    /**
     * L3: 数据库
     */
    public abstract JpaRepository<T, ID> database();

    public <T> T findById(Class<T> clazz, String cacheName, ID id) {
        // 1. 先查本地缓存
        T entity = localCache.getCache(cacheName).get(id, clazz);
        if (entity != null) return entity;

        // 2. 再查Redis
        entity = (T) redisCache.opsForValue().get(cacheName + "_:" + id);
        if (entity != null) {
            // 回填本地缓存
            localCache.getCache(cacheName).put(id, entity);
            return entity;
        }

        // 3. 最后查数据库
        entity = (T) database().findById(id).orElse(null);
        if (entity != null) {
            final T t = entity;
            // 异步回填缓存
            CompletableFuture.runAsync(() -> {
                redisCache.opsForValue().set(cacheName + "_:" + id, t, Duration.ofMinutes(30));
                localCache.getCache(cacheName).put(id, t);
            });
        }
        return entity;
    }
}
