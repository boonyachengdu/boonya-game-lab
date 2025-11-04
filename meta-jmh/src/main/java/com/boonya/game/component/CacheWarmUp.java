package com.boonya.game.component;

import com.boonya.game.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 热点数据加载
 */
@Component
public class CacheWarmUp {

    @Autowired
    private UserService userService;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCache() {
        // 启动时预热热点数据
        List<Long> hotUserIds = userService.getHotUserIds();
        hotUserIds.parallelStream().forEach(id -> {
            userService.getUserMultiLevel(id);
        });
    }

    // 预测性预加载
    @Scheduled(fixedRate = 60000) // 每分钟执行
    public void predictiveLoading() {
        List<Long> predictedHotIds = userService.predictHotUserIds();
        predictedHotIds.parallelStream().forEach(id -> {
            if (!userService.cacheContains(id)) {
                userService.getUserMultiLevel(id);
            }
        });
    }
}
