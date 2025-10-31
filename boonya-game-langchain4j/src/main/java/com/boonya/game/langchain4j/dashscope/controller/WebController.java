package com.boonya.game.langchain4j.dashscope.controller;

import com.boonya.game.langchain4j.dashscope.document.DocumentProcessingService;
import com.boonya.game.langchain4j.dashscope.memory.ConversationMemoryService;
import com.boonya.game.langchain4j.dashscope.rag.RAGService;
import com.boonya.game.langchain4j.security.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
public class WebController {

    private final RAGService ragService;
    private final DocumentProcessingService documentService;
    private final UserSessionService userSessionService;
    private final ConversationMemoryService memoryService;

    // 文件上传目录
    private final Path uploadDir = Paths.get("uploads");

    public WebController(RAGService ragService,
                         DocumentProcessingService documentService,
                         UserSessionService userSessionService,
                         ConversationMemoryService memoryService) {
        this.ragService = ragService;
        this.documentService = documentService;
        this.userSessionService = userSessionService;
        this.memoryService = memoryService;

        // 创建上传目录
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录", e);
        }
    }

    /**
     * 测试API面
     */
    @GetMapping("/test-api")
    public String testApi(Model model) {
        return "test-api";
    }

    /**
     * 登录页面
     */
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("error", "用户名或密码错误");
        }
        if (logout != null) {
            model.addAttribute("message", "您已成功退出");
        }
        return "login";
    }

    /**
     * 主聊天页面
     */
    @GetMapping("/chat")
    public String chat(Model model, @RequestParam(value = "sessionId", required = false) String sessionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();

        // 如果没有提供sessionId，创建新会话
        if (sessionId == null || sessionId.isEmpty()) {
            UserSessionService.UserSession newSession = userSessionService.createSession(username);
            sessionId = newSession.getSessionId();
            return "redirect:/chat?sessionId=" + sessionId;
        }

        // 验证会话属于当前用户 - 使用 userSession 避免保留字冲突
        UserSessionService.UserSession userSession = userSessionService.getSession(sessionId);
        if (userSession == null || !userSession.getUsername().equals(username)) {
            return "redirect:/chat"; // 重新创建会话
        }

        userSessionService.updateSessionActivity(sessionId);

        model.addAttribute("username", username);
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("messageCount", userSession.getMessageCount());

        // 获取用户的所有会话
        List<UserSessionService.UserSession> userSessions = userSessionService.getUserSessions(username)
                .values().stream()
                .sorted((s1, s2) -> Long.compare(s2.getLastActivityTime(), s1.getLastActivityTime()))
                .collect(Collectors.toList());
        model.addAttribute("sessions", userSessions);

        return "chat";
    }

    /**
     * 文档管理页面
     */
    @GetMapping("/documents")
    public String documents(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        model.addAttribute("username", auth.getName());
        return "documents";
    }

    /**
     * 处理文件上传
     */
    @PostMapping("/upload")
    @ResponseBody
    public UploadResponse handleFileUpload(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return new UploadResponse("error", "请选择文件");
            }

            // 保存文件
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            // 处理文档
            documentService.processDocument(filePath);
            log.info("文件上传和处理成功!");
            return new UploadResponse("success", "文件上传和处理成功: " + file.getOriginalFilename());

        } catch (Exception e) {
            log.error("文件上传失败", e);
            return new UploadResponse("error", "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 创建新会话
     */
    @PostMapping("/new-session")
    public String createNewSession() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserSessionService.UserSession session = userSessionService.createSession(auth.getName());
        return "redirect:/chat?sessionId=" + session.getSessionId();
    }

    /**
     * 切换会话
     */
    @PostMapping("/switch-session")
    public String switchSession(@RequestParam String sessionId) {
        return "redirect:/chat?sessionId=" + sessionId;
    }

    /**
     * 结束会话
     */
    @PostMapping("/end-session")
    @ResponseBody
    public Map<String, String> endSession(@RequestParam String sessionId) {
        userSessionService.endSession(sessionId);
        return Map.of("status", "success", "message", "会话已结束");
    }

    // 响应DTO
    public static class UploadResponse {
        private String status;
        private String message;

        public UploadResponse(String status, String message) {
            this.status = status;
            this.message = message;
        }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
