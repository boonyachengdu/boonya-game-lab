package com.boonya.game.ai.rag.base.controller;

import com.boonya.game.ai.rag.base.service.RagService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final RagService ragService;

    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping
    public String chat(@RequestBody ChatRequest request) {
        return ragService.generateAnswer(request.question());
    }

    // 简单的请求体记录
    public record ChatRequest(String question) {
    }
}