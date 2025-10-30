package com.boonya.game.langchain4j.dashscope.rag;

import com.boonya.game.langchain4j.dashscope.memory.ConversationMemoryService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.retriever.Retriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;

@Service
public class RAGService {

    private final Assistant assistant;
    private final ConversationMemoryService memoryService;

    public RAGService(ChatLanguageModel chatModel,
                      EmbeddingModel embeddingModel,
                      EmbeddingStore<TextSegment> embeddingStore,
                      ConversationMemoryService memoryService) {

        this.memoryService = memoryService;

        // 创建检索器
        Retriever<TextSegment> retriever = EmbeddingStoreRetriever.from(embeddingStore, embeddingModel, 3);

        // 构建AI服务，集成检索器和记忆
        this.assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(chatModel)
                .retriever(retriever)
                .chatMemoryProvider(memoryService::getOrCreateMemory)
                .build();
    }

    /**
     * 基于文档的问答
     */
    public String askQuestion(String sessionId, String question) {
        try {
            // 记录用户问题
            memoryService.addUserMessage(sessionId, question);

            // 获取AI回复（自动包含检索和记忆）
            String answer = assistant.chat(sessionId, question);

            // 记录AI回复
            memoryService.addAiMessage(sessionId, answer);

            return answer;

        } catch (Exception e) {
            throw new RuntimeException("问答处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取对话历史
     */
    public String getConversationHistory(String sessionId) {
        return memoryService.getConversationHistory(sessionId);
    }

    /**
     * 开始新对话
     */
    public void startNewConversation(String sessionId) {
        memoryService.clearMemory(sessionId);
    }
}
