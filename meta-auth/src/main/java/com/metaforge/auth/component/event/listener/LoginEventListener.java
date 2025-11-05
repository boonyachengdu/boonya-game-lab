package com.metaforge.auth.component.event.listener;

import com.metaforge.auth.component.cache.StatisticsCache;
import com.metaforge.auth.component.event.LoginEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginEventListener implements ApplicationListener<LoginEvent> {

    private final StatisticsCache statisticsCache;

    @Override
    public void onApplicationEvent(LoginEvent event) {
        statisticsCache.evict();
    }
}
