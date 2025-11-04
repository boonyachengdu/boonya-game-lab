package com.metaforage.cache.mode;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 */
public class CacheConfig {
    private final CacheMode cacheMode;
    private final long maximumSize;
    private final long expireAfterWrite; // 写入后过期时间
    private final long expireAfterAccess; // 访问后过期时间
    private final TimeUnit timeUnit;
    private final boolean recordStats; // 是否记录统计信息

    private CacheConfig(Builder builder) {
        this.cacheMode = builder.cacheMode;
        this.maximumSize = builder.maximumSize;
        this.expireAfterWrite = builder.expireAfterWrite;
        this.expireAfterAccess = builder.expireAfterAccess;
        this.timeUnit = builder.timeUnit;
        this.recordStats = builder.recordStats;
    }

    public static Builder builder() {
        return new Builder();
    }


    // Getters...
    public CacheMode getCacheMode() {
        return cacheMode;
    }

    public long getMaximumSize() {
        return maximumSize;
    }

    public long getExpireAfterWrite() {
        return expireAfterWrite;
    }

    public long getExpireAfterAccess() {
        return expireAfterAccess;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public boolean isRecordStats() {
        return recordStats;
    }

    public static class Builder {
        private CacheMode cacheMode = CacheMode.LOCAL;
        private long maximumSize = 1000;
        private long expireAfterWrite = -1;
        private long expireAfterAccess = -1;
        private TimeUnit timeUnit = TimeUnit.SECONDS;
        private boolean recordStats = false;

        public Builder cacheMode(CacheMode cacheMode) {
            this.cacheMode = cacheMode;
            return this;
        }

        public Builder maximumSize(long maximumSize) {
            this.maximumSize = maximumSize;
            return this;
        }

        public Builder expireAfterWrite(long duration, TimeUnit timeUnit) {
            this.expireAfterWrite = duration;
            this.timeUnit = timeUnit;
            return this;
        }

        public Builder expireAfterAccess(long duration, TimeUnit timeUnit) {
            this.expireAfterAccess = duration;
            this.timeUnit = timeUnit;
            return this;
        }

        public Builder recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }

        public CacheConfig build() {
            return new CacheConfig(this);
        }
    }
}