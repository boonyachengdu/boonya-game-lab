package com.metaforge.auth.component.event;

import org.springframework.context.ApplicationEvent;

public class UserChangeEvent extends ApplicationEvent {

    public UserChangeEvent(Object source) {
        super(source);
    }

    public UserChangeEvent createUserCreatedEvent(Long id) {
        return new UserChangeEvent(new UserChangeEventSource(id));
    }

    public static class UserChangeEventSource {
        private Long id;

        public UserChangeEventSource(Long id) {
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }
    }
}
