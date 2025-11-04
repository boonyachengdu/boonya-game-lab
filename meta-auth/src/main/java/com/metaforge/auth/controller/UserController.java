package com.metaforge.auth.controller;

import com.metaforage.cache.mode.CacheStats;
import com.metaforge.auth.entity.User;
import com.metaforge.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/cache/stats")
    public CacheStats getCacheStats() {
        return userService.getUserCache().getStats();
    }
}
