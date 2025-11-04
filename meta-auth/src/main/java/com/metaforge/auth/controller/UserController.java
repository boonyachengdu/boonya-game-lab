package com.metaforge.auth.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.metaforage.cache.mode.CacheStats;
import com.metaforge.auth.entity.User;
import com.metaforge.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/profile")
    public String getProfile(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = userService.findByUsernameBasic(authentication.getName()).orElse(null);
        log.debug("User profile: {}", user);
        model.addAttribute("user", Optional.ofNullable(user).orElse(new User()));

        model.addAttribute("userActivities", Lists.newArrayList());

        Map<String, Object> userStats = Maps.newHashMap();
        userStats.put("loginCount", 100);
        userStats.put("daysSinceJoin", 1000);
        userStats.put("lastActiveDays", 20);
        userStats.put("securityScore", 95);
        model.addAttribute("userStats", userStats);
        return "profile";
    }

    @GetMapping("/users/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @GetMapping("/cache/stats")
    public CacheStats getCacheStats() {
        return userService.getUserCache().getStats();
    }
}
