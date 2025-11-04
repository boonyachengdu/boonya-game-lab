package com.metaforage.cache.mode;

/**
 * 缓存统计信息
 */
public class CacheStats {
    private final long hitCount;
    private final long missCount;
    private final long loadSuccessCount;
    private final long loadFailureCount;
    private final long totalLoadTime;
    private final long evictionCount;

    public CacheStats(long hitCount, long missCount, long loadSuccessCount,
                      long loadFailureCount, long totalLoadTime, long evictionCount) {
        this.hitCount = hitCount;
        this.missCount = missCount;
        this.loadSuccessCount = loadSuccessCount;
        this.loadFailureCount = loadFailureCount;
        this.totalLoadTime = totalLoadTime;
        this.evictionCount = evictionCount;
    }

    // Getters...
    public long getHitCount() { return hitCount; }
    public long getMissCount() { return missCount; }
    public long getLoadSuccessCount() { return loadSuccessCount; }
    public long getLoadFailureCount() { return loadFailureCount; }
    public long getTotalLoadTime() { return totalLoadTime; }
    public long getEvictionCount() { return evictionCount; }

    public long getRequestCount() {
        return hitCount + missCount;
    }

    public double getHitRate() {
        long requestCount = getRequestCount();
        return requestCount == 0 ? 1.0 : (double) hitCount / requestCount;
    }

    public double getMissRate() {
        long requestCount = getRequestCount();
        return requestCount == 0 ? 0.0 : (double) missCount / requestCount;
    }
}
