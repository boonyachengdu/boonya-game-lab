package com.metaforge.auth.component.cache;

import com.metaforage.cache.AbstractCache;
import com.metaforage.cache.Cache;
import com.metaforage.cache.component.CacheManager;
import com.metaforage.cache.mode.CacheConfig;
import com.metaforge.auth.dto.response.Statistics;
import com.metaforge.auth.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StatisticsCache extends AbstractCache<String, Statistics> {

    private final StatisticsService statisticsService;
    private final CacheManager cacheManager;
    private final CacheConfig cacheConfig;
    public static final String STATISTICS = "statistics";


    @Override
    public Cache<String, Statistics> getCache() {
        return super.setLocalOrDistributedCache(STATISTICS, cacheConfig, cacheManager);
    }

    public Statistics getStatistics() {
        Statistics cachedStatistics = getCache().get(STATISTICS);
        if (cachedStatistics != null) {
            return cachedStatistics;
        }
        Statistics statistics = new Statistics();
        statistics.setTotalUsers(statisticsService.getTotalUsers());
        statistics.setTotalRoles(statisticsService.getTotalRoles());
        statistics.setActiveUsers(statisticsService.getTotalEnabledUsers());
        statistics.setTotalUsers(statisticsService.getTotalUsers());
        statistics.setTodayLogins(statisticsService.getTodayLogins());
        getCache().put(STATISTICS, statistics);
        return statistics;
    }

    public void evict() {
        getCache().evict(STATISTICS);
    }
}
