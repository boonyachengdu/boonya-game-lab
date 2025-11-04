package com.metaforge.auth.controller;

import com.metaforge.auth.entity.Role;
import com.metaforge.auth.entity.User;
import com.metaforge.auth.service.AuthService;
import com.metaforge.auth.service.RoleService;
import com.metaforge.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userManagementService;
    private final RoleService roleService;
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_ROLE = "USER";
    private final AuthService authService;

    /**
     * 显示登录页面
     */
    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "error", required = false) String error,
                                @RequestParam(value = "logout", required = false) String logout,
                                Model model) {

        if (error != null) {
            model.addAttribute("error", true);
        }

        if (logout != null) {
            model.addAttribute("logout", true);
        }

        return "login";
    }

    /**
     * REST API 登录端点
     * 注意：表单登录由 Spring Security 处理，这个端点用于 REST API 调用
     */
    @PostMapping("/api/login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiLogin(@RequestBody LoginRequest loginRequest,
                                                        HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        boolean isAuthenticated = authService.authenticate(
                loginRequest.getUsername(),
                loginRequest.getPassword()
        );

        if (isAuthenticated) {
            // 创建或获取 HttpSession，并将 SecurityContext 存储到 Session 中
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

            response.put("success", true);
            response.put("message", "Login successful");
            response.put("username", loginRequest.getUsername());

            log.info("API login successful for user: {}", loginRequest.getUsername());
        } else {
            log.error("API login failed for user: {}", loginRequest.getUsername());
            response.put("success", false);
            response.put("message", "Login failed");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 验证当前用户会话
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateSession() {
        Map<String, Object> response = new HashMap<>();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            response.put("authenticated", true);
            response.put("username", authentication.getName());
            response.put("authorities", authentication.getAuthorities());
        } else {
            response.put("authenticated", false);
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 显示注册页面
     */
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    /**
     * 处理用户注册
     */
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {

        // 检查表单验证错误
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "请检查表单输入是否正确");
            return "register";
        }

        try {
            // 检查用户名是否已存在
            if (userManagementService.usernameExists(registerRequest.getUsername())) {
                model.addAttribute("error", "用户名已存在");
                return "register";
            }

            // 检查邮箱是否已存在
            if (userManagementService.emailExists(registerRequest.getEmail())) {
                model.addAttribute("error", "邮箱地址已被使用");
                return "register";
            }

            // 检查密码确认
            if (!registerRequest.getPassword().equals(registerRequest.getConfirmPassword())) {
                model.addAttribute("error", "密码不匹配");
                return "register";
            }

            // 创建用户
            Set<Role> roles = roleService.getRolesByNameIn(Set.of(USER_ROLE));
            User newUser = userManagementService.createUser(
                    registerRequest.getUsername(),
                    registerRequest.getPassword(),
                    registerRequest.getEmail(),
                    roles.stream().map(Role::getId).collect(Collectors.toSet())
            );

            log.info("新用户注册成功: {}", newUser.getUsername());

            // 注册成功，重定向到登录页面
            redirectAttributes.addFlashAttribute("success", true);
            return "redirect:/login";

        } catch (Exception e) {
            log.error("用户注册失败: {}", e.getMessage(), e);
            model.addAttribute("error", "注册失败: " + e.getMessage());
            return "register";
        }
    }

    /**
     * 处理忘记密码请求
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    /**
     * 处理忘记密码提交
     */
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            // 这里可以实现发送重置密码邮件的逻辑
            log.info("收到密码重置请求: {}", email);

            redirectAttributes.addFlashAttribute("message",
                    "如果该邮箱已注册，我们将发送重置密码链接到您的邮箱");
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "处理请求时发生错误");
            return "redirect:/forgot-password";
        }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        // Getters and Setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * 注册请求DTO
     */
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String confirmPassword;

        // Getters and Setters
        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }
}
