package com.metaforge.auth.component;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SessionListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        log.debug("会话创建: {}", se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        log.debug("会话销毁: {}", se.getSession().getId());
    }
}