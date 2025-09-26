package com.boonya.game.ai.rag.base.service;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG知识库服务
 */
@Service
public class RagService {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    // 从 classpath 加载系统提示词模板
    @Value("classpath:/prompts/system-rag.st")
    private Resource systemPromptResource;

    public RagService(ChatModel chatModel, VectorStore vectorStore) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
    }

    public String generateAnswer(String userQuestion) {
        // 1. 检索：从向量库中查找最相关的文档片段
        SearchRequest searchRequest = SearchRequest.builder()
                .query(userQuestion)
                .topK(5)// 返回最相关的5个片段
                .build();

        List<Document> similarDocuments = vectorStore.similaritySearch(searchRequest);

        // 2. 构建上下文：将检索到的文档内容合并
        String context = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // 3. 构建系统提示词（使用Apache FreeMarker模板）
        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPromptResource);
        String systemMessage = systemPromptTemplate.render(Map.of("context", context));

        // 4. 构建完整的对话提示
        Prompt prompt = new Prompt(
                List.of(
                        systemPromptTemplate.createMessage(), // 系统消息
                        new UserMessage(userQuestion) // 用户问题
                )
        );

        // 5. 调用大模型生成答案
        return chatModel.call(prompt).getResult().getOutput().getText();
    }
}
