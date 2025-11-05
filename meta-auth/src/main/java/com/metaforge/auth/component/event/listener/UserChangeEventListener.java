package com.metaforge.auth.component.event.listener;

import com.metaforge.auth.component.cache.UserCache;
import com.metaforge.auth.component.event.UserChangeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserChangeEventListener implements ApplicationListener<UserChangeEvent> {

    private final UserCache userCache;

    @Override
    public void onApplicationEvent(UserChangeEvent event) {
        UserChangeEvent.UserChangeEventSource source = (UserChangeEvent.UserChangeEventSource) event.getSource();
        userCache.getCache().evict(source.getId());
    }
}
