package com.metaforge.auth.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.metaforge.auth.dto.response.Statistics;
import com.metaforge.auth.entity.User;
import com.metaforge.auth.service.StatisticsService;
import com.metaforge.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequiredArgsConstructor
public class IndexController {

    private final StatisticsService statisticsService;
    private final UserService userService;

    /**
     * 显示首页
     */
    @GetMapping("/")
    public String showIndexPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        if (!auth.isAuthenticated() || "anonymousUser".equals(username)) {
            return "redirect:/login";
        }
        User user = userService.findByUsernameBasic(username).orElseThrow(() -> new RuntimeException("用户不存在"));
        model.addAttribute("user", user);
        model.addAttribute("username", username);
        model.addAttribute("recentActivities", Lists.newArrayList());
        model.addAttribute("stats", statisticsService.getStatistics());
        return "index";
    }

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

    /**
     * 获取使用统计 API
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public Statistics getStats() {
        return statisticsService.getStatistics();
    }
}
