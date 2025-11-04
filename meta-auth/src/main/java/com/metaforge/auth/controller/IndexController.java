package com.metaforge.auth.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class IndexController {

    /**
     * 显示首页
     */
    @GetMapping("/")
    public String showIndexPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        model.addAttribute("username", username);
        log.info("用户访问首页: {}", username);

        if (!auth.isAuthenticated() || "anonymousUser".equals(username)) {
            return "redirect:/login";
        }

        return "index";
    }

    /**
     * 获取用户信息 API
     */
    @GetMapping("/api/user/info")
    @ResponseBody
    public Map<String, Object> getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", username);
        userInfo.put("roles", auth.getAuthorities());
        userInfo.put("authenticated", auth.isAuthenticated());

        log.debug("获取用户信息: {}", username);
        return userInfo;
    }

    /**
     * 获取使用统计 API
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public Map<String, Object> getStats() {
        // 这里可以连接数据库获取真实的统计数据
        Map<String, Object> stats = new HashMap<>();
        stats.put("chatCount", 42);
        stats.put("documentCount", 15);
        stats.put("sessionCount", 3);
        stats.put("accuracyRate", 92);

        return stats;
    }
}
