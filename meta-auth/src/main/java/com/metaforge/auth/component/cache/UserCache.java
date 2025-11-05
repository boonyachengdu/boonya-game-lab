package com.metaforge.auth.component.cache;

import com.metaforage.cache.AbstractCache;
import com.metaforage.cache.Cache;
import com.metaforage.cache.component.CacheManager;
import com.metaforage.cache.mode.CacheConfig;
import com.metaforge.auth.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCache extends AbstractCache<Long, User> {

    private final CacheManager cacheManager;
    private final CacheConfig cacheConfig;

    private static final String USERS = "users";

    @Override
    public Cache<Long, User> getCache() {
        return super.setLocalOrDistributedCache(USERS, cacheConfig, cacheManager);
    }
}
