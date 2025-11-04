package com.metaforge.auth.config;

import com.metaforage.cache.component.CacheManager;
import com.metaforage.cache.mode.CacheConfig;
import com.metaforage.cache.mode.CacheMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheModeConfig {

    @Value("${cache.mode:LOCAL}")
    private CacheMode cacheMode;

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        return template;
    }

    private RedisConnectionFactory redisConnectionFactory() {
        return new org.springframework.data.redis.connection.jedis.JedisConnectionFactory();
    }

    @Bean
    public CacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
        return new CacheManager(redisTemplate);
    }

    @Bean
    public CacheConfig cacheConfig() {
        return CacheConfig.builder()
                .cacheMode(cacheMode)
                .maximumSize(1000)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .recordStats(true)
                .build();
    }
}
