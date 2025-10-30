package com.boonya.game.langchain4j.dashscope.controller;

// RAGController.java
import com.boonya.game.langchain4j.dashscope.document.DocumentProcessingService;
import com.boonya.game.langchain4j.dashscope.rag.RAGService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RAGController {

    private final RAGService ragService;
    private final DocumentProcessingService documentService;

    public RAGController(RAGService ragService, DocumentProcessingService documentService) {
        this.ragService = ragService;
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestParam String filePath) {
        try {
            documentService.processDocument(Paths.get(filePath));
            return ResponseEntity.ok("文档上传和处理成功");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("文档处理失败: " + e.getMessage());
        }
    }

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> askQuestion(
            @RequestParam String sessionId,
            @RequestParam String question) {
        try {
            String answer = ragService.askQuestion(sessionId, question);
            return ResponseEntity.ok(new ChatResponse(answer, "success"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse(null, "error: " + e.getMessage()));
        }
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<String> getHistory(@PathVariable String sessionId) {
        try {
            String history = ragService.getConversationHistory(sessionId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("获取历史失败: " + e.getMessage());
        }
    }

    @PostMapping("/new-session/{sessionId}")
    public ResponseEntity<String> newSession(@PathVariable String sessionId) {
        ragService.startNewConversation(sessionId);
        return ResponseEntity.ok("新对话会话已创建");
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
