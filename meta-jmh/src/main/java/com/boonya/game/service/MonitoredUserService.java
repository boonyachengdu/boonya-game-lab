package com.boonya.game.service;

import com.boonya.game.cache.CacheService;
import com.boonya.game.dao.UserRepository;
import com.boonya.game.model.User;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.*;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

@Service
public class MonitoredUserService {

    // 业务组件
    @Resource
    private UserRepository userRepository;
    @Resource
    private CacheService cacheService;

    // 监控指标
    private final Counter queryCounter;
    private final Timer queryTimer;
    private final Counter errorCounter;
    private final Gauge cacheHitGauge;
    private final DistributionSummary responseSizeSummary;

    // 性能统计
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);
    private final AtomicLong errors = new AtomicLong(0);

    // 本地缓存
    private final ConcurrentHashMap<Long, User> localCache = new ConcurrentHashMap<>(10000);
    private final StampedLock cacheLock = new StampedLock();

    @Autowired
    public MonitoredUserService(MeterRegistry registry) {
        // 初始化监控指标
        this.queryCounter = Counter.builder("user.query.count")
                .description("Total number of user queries")
                .tag("service", "user-service")
                .register(registry);

        this.queryTimer = Timer.builder("user.query.duration")
                .description("Time taken for user queries")
                .tag("service", "user-service")
                .publishPercentiles(0.5, 0.95, 0.99) // 50th, 95th, 99th percentiles
                .register(registry);

        this.errorCounter = Counter.builder("user.query.errors")
                .description("Number of failed user queries")
                .tag("service", "user-service")
                .register(registry);

        this.responseSizeSummary = DistributionSummary.builder("user.response.size")
                .description("Size of user response data")
                .baseUnit("bytes")
                .register(registry);

        // TODO 注册缓存命中率指标
        this.cacheHitGauge = Gauge.builder("user.cache.hit.ratio",null)
                .description("Cache hit ratio for user queries")
                .tag("service", "user-service")
                .register(registry);
    }

    /**
     * 监控的用户查询方法 - 包含完整的多级缓存和性能监控
     */
    @Timed(value = "user.query", description = "Time taken to query user with caching")
    public User getMonitoredUser(Long id) {
        if (id == null || id <= 0) {
            recordError("invalid_id");
            throw new IllegalArgumentException("Invalid user ID: " + id);
        }

        totalQueries.incrementAndGet();
        queryCounter.increment();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        try {
            User user = queryTimer.record(() -> getUserWithCaching(id));

            stopWatch.stop();
            recordSuccess(stopWatch.getTotalTimeMillis(), user);

            return user;

        } catch (Exception e) {
            stopWatch.stop();
            recordError(e.getClass().getSimpleName());
            throw new RuntimeException("Failed to get user: " + id, e);
        }
    }

    /**
     * 带有多级缓存的用户查询实现
     */
    private User getUserWithCaching(Long id) {
        // 1. 检查本地缓存 (L1缓存)
        User user = getFromLocalCache(id);
        if (user != null) {
            cacheHits.incrementAndGet();
            return user;
        }

        // 2. 检查Redis缓存 (L2缓存)
        user = cacheService.getFromRedis("user:" + id, User.class);
        if (user != null) {
            cacheHits.incrementAndGet();
            // 回填本地缓存
            putToLocalCache(id, user);
            return user;
        }

        cacheMisses.incrementAndGet();

        // 3. 查询数据库
        user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        // 4. 异步更新缓存
        updateCachesAsync(id, user);

        return user;
    }

    /**
     * 从本地缓存获取用户
     */
    private User getFromLocalCache(Long id) {
        long stamp = cacheLock.tryOptimisticRead();
        User user = localCache.get(id);

        if (!cacheLock.validate(stamp)) {
            stamp = cacheLock.readLock();
            try {
                user = localCache.get(id);
            } finally {
                cacheLock.unlockRead(stamp);
            }
        }

        return user;
    }

    /**
     * 将用户放入本地缓存
     */
    private void putToLocalCache(Long id, User user) {
        long stamp = cacheLock.writeLock();
        try {
            localCache.put(id, user);
        } finally {
            cacheLock.unlockWrite(stamp);
        }
    }

    /**
     * 异步更新多级缓存
     */
    private void updateCachesAsync(Long id, User user) {
        // 使用CompletableFuture异步更新缓存，不阻塞主线程
        CompletableFuture.runAsync(() -> {
            try {
                // 更新本地缓存
                putToLocalCache(id, user);

                // 更新Redis缓存，设置30分钟过期
                cacheService.putToRedis("user:" + id, user, 30, TimeUnit.MINUTES);

            } catch (Exception e) {
                // 缓存更新失败不影响主流程，但记录日志
                System.err.println("Failed to update cache for user " + id + ": " + e.getMessage());
            }
        });
    }

    /**
     * 批量查询用户 - 优化批量操作的性能
     */
    @Timed(value = "user.batch.query", description = "Time taken for batch user queries")
    public List<User> getUsersBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        Counter.builder("user.batch.query.count")
                .tag("batch_size", String.valueOf(ids.size()))
                .register(Metrics.globalRegistry)
                .increment();

        return queryTimer.record(() -> {
            List<User> result = new ArrayList<>(ids.size());

            // 分批处理，避免单次查询数据量过大
            List<List<Long>> batches = Lists.partition(ids, 100);

            batches.parallelStream().forEach(batch -> {
                List<User> batchResult = processUserBatch(batch);
                synchronized (result) {
                    result.addAll(batchResult);
                }
            });

            return result;
        });
    }

    private List<User> processUserBatch(List<Long> batchIds) {
        Map<Long, User> cachedUsers = new HashMap<>();
        List<Long> missingIds = new ArrayList<>();

        // 先检查缓存
        for (Long id : batchIds) {
            User cached = getFromLocalCache(id);
            if (cached != null) {
                cachedUsers.put(id, cached);
            } else {
                missingIds.add(id);
            }
        }

        // 查询缺失的数据
        if (!missingIds.isEmpty()) {
            List<User> dbUsers = userRepository.findAllById(missingIds);
            for (User user : dbUsers) {
                cachedUsers.put(user.getId(), user);
                updateCachesAsync(user.getId(), user);
            }
        }

        // 按原始顺序返回结果
        return batchIds.stream()
                .map(cachedUsers::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 记录成功查询的指标
     */
    private void recordSuccess(long durationMs, User user) {
        // 记录响应大小（估算）
        if (user != null) {
            int estimatedSize = estimateUserSize(user);
            responseSizeSummary.record(estimatedSize);
        }

        // 记录慢查询
        if (durationMs > 1000) { // 超过1秒认为是慢查询
            Counter.builder("user.query.slow")
                    .tag("duration_range", "over_1s")
                    .register(Metrics.globalRegistry)
                    .increment();
        }
    }

    /**
     * 记录错误指标
     */
    private void recordError(String errorType) {
        errors.incrementAndGet();
        errorCounter.increment();

        Counter.builder("user.query.error.types")
                .tag("error_type", errorType)
                .register(Metrics.globalRegistry)
                .increment();
    }

    /**
     * 估算用户对象大小（用于监控）
     */
    private int estimateUserSize(User user) {
        if (user == null) return 0;

        int size = 0;
        size += user.getId() != null ? 8 : 0;
        size += user.getName() != null ? user.getName().length() * 2 : 0;
        size += user.getEmail() != null ? user.getEmail().length() * 2 : 0;
        // 添加其他字段的大小估算

        return Math.max(size, 100); // 最小100字节
    }

    /**
     * 获取性能统计信息
     */
    public Map<String, Object> getPerformanceStats() {
        long total = totalQueries.get();
        long hits = cacheHits.get();

        return Map.of(
                "totalQueries", total,
                "cacheHits", hits,
                "cacheMisses", cacheMisses.get(),
                "errors", errors.get(),
                "cacheHitRatio", total > 0 ? String.format("%.2f%%", (double) hits / total * 100) : "0%",
                "localCacheSize", localCache.size()
        );
    }

    /**
     * 清理缓存（用于测试和管理）
     */
    public void clearCache() {
        long stamp = cacheLock.writeLock();
        try {
            localCache.clear();
        } finally {
            cacheLock.unlockWrite(stamp);
        }

        cacheService.clearUserCache();
    }

    /**
     * 预热缓存
     */
    public void warmUpCache(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return;

        Timer timer = Timer.builder("user.cache.warmup")
                .register(Metrics.globalRegistry);

        timer.record(() -> {
            userIds.parallelStream().forEach(id -> {
                try {
                    getMonitoredUser(id);
                } catch (Exception e) {
                    // 忽略预热过程中的错误
                }
            });
        });
    }
}