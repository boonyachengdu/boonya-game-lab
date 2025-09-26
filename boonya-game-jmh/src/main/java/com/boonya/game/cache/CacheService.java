package com.boonya.game.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

public interface CacheService {
    <T> T getFromRedis(String key, Class<T> type);

    void putToRedis(String key, Object value, long timeout, TimeUnit unit);

    void clearUserCache();

    @Service("cacheService")
    public class CacheServiceImpl implements CacheService {
        private static final String USER_CACHE_KEY = "user:";

        @Autowired
        private RedisTemplate redisTemplate;

        @Override
        public <T> T getFromRedis(String key, Class<T> type) {
            return (T) redisTemplate.opsForValue().get(key);
        }

        @Override
        public void putToRedis(String key, Object value, long timeout, TimeUnit unit) {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        }

        @Override
        public void clearUserCache() {
            redisTemplate.opsForValue().set(USER_CACHE_KEY, null);
        }
    }
}
