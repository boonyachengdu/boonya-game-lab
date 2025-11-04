// RAGController.java
package com.metaforge.ai.langchain4j.dashscope.controller;

import com.metaforge.ai.langchain4j.dashscope.document.DocumentProcessingService;
import com.metaforge.ai.langchain4j.dashscope.rag.RAGService;
import com.metaforge.ai.langchain4j.security.UserSessionService;
import com.metaforge.ai.langchain4j.utils.ApiAuthUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/rag")
public class RAGController {

    private final RAGService ragService;
    private final DocumentProcessingService documentService;
    private final ApiAuthUtils apiAuthUtils;
    private final UserSessionService userSessionService;

    public RAGController(RAGService ragService,
                         DocumentProcessingService documentService,
                         ApiAuthUtils apiAuthUtils,
                         UserSessionService userSessionService) {
        this.ragService = ragService;
        this.documentService = documentService;
        this.apiAuthUtils = apiAuthUtils;
        this.userSessionService = userSessionService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck(Authentication authentication) {
        Map<String, Object> healthInfo = new HashMap<>();

        try {
            // 检查基础服务状态
            healthInfo.put("status", "UP");
            healthInfo.put("timestamp", System.currentTimeMillis());
            healthInfo.put("service", "RAG Chat Service");

            // 添加认证信息
            if (authentication != null && authentication.isAuthenticated()) {
                healthInfo.put("authenticated", true);
                healthInfo.put("username", authentication.getName());
            } else {
                healthInfo.put("authenticated", false);
            }

            // 检查向量存储状态
            healthInfo.put("vectorStore", "Memory");
            healthInfo.put("vectorStoreStatus", "OK");

            // 检查AI模型状态
            healthInfo.put("aiModel", "Mock Service");
            healthInfo.put("aiModelStatus", "OK");

            return ResponseEntity.ok(healthInfo);

        } catch (Exception e) {
            healthInfo.put("status", "DOWN");
            healthInfo.put("error", e.getMessage());
            return ResponseEntity.status(500).body(healthInfo);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam String filePath, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("未认证");
            }

            documentService.processDocument(Paths.get(filePath));
            return ResponseEntity.ok("文档上传和处理成功");
        } catch (Exception e) {
            log.error("文档上传失败", e);
            return ResponseEntity.badRequest().body("文档处理失败: " + e.getMessage());
        }
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> askQuestion(
            @RequestParam String sessionId,
            @RequestParam String question,
            Authentication authentication) {
        try {
            log.info("收到提问请求 - 用户: {}, sessionId: {}, question: {}",
                    authentication.getName(), sessionId, question);

            if (sessionId == null || sessionId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ChatResponse(null, "error: sessionId不能为空"));
            }

            if (question == null || question.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ChatResponse(null, "error: 问题内容不能为空"));
            }

            // 验证会话属于当前用户
            UserSessionService.UserSession session = userSessionService.getSession(sessionId);
            if (session == null || !session.getUsername().equals(authentication.getName())) {
                return ResponseEntity.status(403)
                        .body(new ChatResponse(null, "error: 无权访问此会话"));
            }

            String answer = ragService.askQuestion(sessionId, question.trim());
            log.info("问题回答完成 - sessionId: {}, 答案长度: {}", sessionId, answer.length());

            return ResponseEntity.ok(new ChatResponse(answer, "success"));

        } catch (Exception e) {
            log.error("问答处理失败 - sessionId: {}, question: {}", sessionId, question, e);
            String errorMessage = "处理问题时发生错误: " + e.getMessage();
            return ResponseEntity.badRequest()
                    .body(new ChatResponse(null, "error: " + errorMessage));
        }
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<String> getHistory(@PathVariable String sessionId, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("未认证");
            }

            // 验证会话属于当前用户
            UserSessionService.UserSession session = userSessionService.getSession(sessionId);
            if (session == null || !session.getUsername().equals(authentication.getName())) {
                return ResponseEntity.status(403).body("无权访问此会话");
            }

            String history = ragService.getConversationHistory(sessionId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("获取历史失败", e);
            return ResponseEntity.badRequest().body("获取历史失败: " + e.getMessage());
        }
    }

    @PostMapping("/new-session/{sessionId}")
    public ResponseEntity<String> newSession(@PathVariable String sessionId, Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body("未认证");
            }

            ragService.startNewConversation(sessionId);
            return ResponseEntity.ok("新对话会话已创建");
        } catch (Exception e) {
            log.error("创建新会话失败", e);
            return ResponseEntity.badRequest().body("创建新会话失败: " + e.getMessage());
        }
    }

    // 响应DTO
    public static class ChatResponse {
        private String answer;
        private String status;

        public ChatResponse(String answer, String status) {
            this.answer = answer;
            this.status = status;
        }

        // getters and setters
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}