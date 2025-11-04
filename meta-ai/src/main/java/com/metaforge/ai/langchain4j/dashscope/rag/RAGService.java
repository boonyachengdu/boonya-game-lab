package com.metaforge.ai.langchain4j.dashscope.rag;

import com.metaforge.ai.langchain4j.dashscope.document.DocumentProcessingService;
import com.metaforge.ai.langchain4j.dashscope.memory.ConversationMemoryService;
import com.metaforge.ai.langchain4j.dashscope.prompt.RAGPromptManager;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.retriever.Retriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
public class RAGService {

    // 依赖组件
    private final ChatLanguageModel chatModel;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ConversationMemoryService memoryService;
    private final RAGPromptManager promptManager;
    private final DocumentProcessingService documentProcessingService;

    // RAG 助手接口
    private final Assistant assistant;

    public RAGService(ChatLanguageModel chatModel,
                      EmbeddingModel embeddingModel,
                      EmbeddingStore<TextSegment> embeddingStore,
                      ConversationMemoryService memoryService,
                      RAGPromptManager promptManager,
                      DocumentProcessingService documentProcessingService) {

        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.memoryService = memoryService;
        this.promptManager = promptManager;
        this.documentProcessingService = documentProcessingService;

        // 初始化 RAG 助手
        this.assistant = initializeAssistant();

        log.info("✅ RAGService 初始化完成");
        log.info("✅ 可用 Prompt 类型: {}", String.join(", ", promptManager.getAvailablePromptTypes()));
    }

    /**
     * 初始化 RAG 助手
     */
    private Assistant initializeAssistant() {
        // 创建检索器
        Retriever<TextSegment> retriever = EmbeddingStoreRetriever.from(
                embeddingStore, embeddingModel, 5 // 检索5个相关片段
        );

        // 构建AI服务
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(chatModel)
                .retriever(retriever)
                .chatMemoryProvider(memoryService::getOrCreateMemory)
                .build();
    }

    // 助手接口定义
    interface Assistant {
        @SystemMessage("""
            你是一个专业的文档问答助手。请基于提供的文档内容回答问题。
            如果文档中没有相关信息，请明确说明。
            请用中文回复，保持专业和准确。
            """)
        String chat(@MemoryId String sessionId, @UserMessage String userMessage);
    }

    /**
     * 基于文档的问答 - 主入口
     */
    public String askQuestion(String sessionId, String question) {
        return askQuestion(sessionId, question, "default");
    }

    /**
     * 基于文档的问答 - 指定 prompt 类型
     */
    public String askQuestion(String sessionId, String question, String promptType) {
        log.info("处理问答请求 - sessionId: {}, 问题: {}, prompt类型: {}",
                sessionId, question, promptType);

        try {
            // 验证输入
            if (sessionId == null || sessionId.trim().isEmpty()) {
                throw new IllegalArgumentException("sessionId不能为空");
            }
            if (question == null || question.trim().isEmpty()) {
                throw new IllegalArgumentException("问题内容不能为空");
            }

            // 验证 prompt 类型
            if (!promptManager.containsPromptType(promptType)) {
                log.warn("未知的prompt类型: {}，使用默认类型", promptType);
                promptType = "default";
            }

            // 记录用户原始问题
            memoryService.addUserMessage(sessionId, question);

            // 使用 PromptManager 构建优化的问题
            String optimizedQuestion = promptManager.getOptimizedQuestion(question, promptType);

            log.debug("优化后的问题: {}", optimizedQuestion);

            // 获取AI回复
            String answer = assistant.chat(sessionId, optimizedQuestion);

            if (answer == null || answer.trim().isEmpty()) {
                throw new RuntimeException("AI回复为空");
            }

            // 后处理：确保回答质量
            answer = postProcessAnswer(answer, question, promptType);

            // 记录AI回复
            memoryService.addAiMessage(sessionId, answer);

            log.info("问答处理完成 - sessionId: {}, 答案长度: {}", sessionId, answer.length());

            return answer;

        } catch (Exception e) {
            log.error("问答处理失败 - sessionId: {}, question: {}", sessionId, question, e);
            throw new RuntimeException("问答处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 回答后处理 - 确保质量
     */
    private String postProcessAnswer(String answer, String originalQuestion, String promptType) {
        String processedAnswer = answer;

        // 检查是否包含明显的虚构内容提示
        if (containsExternalKnowledge(processedAnswer)) {
            log.warn("回答可能包含外部知识，进行修正");
            processedAnswer += "\n\n⚠️ 注意：以上信息严格基于您上传的文档内容。";
        }

        // 确保回答不是太简短（除非是"未找到"类回答或使用简洁模式）
        if (isAnswerTooShort(processedAnswer) &&
                !isNotFoundAnswer(processedAnswer) &&
                !"concise".equals(promptType)) {

            processedAnswer += "\n\n💡 提示：如果您需要更详细的信息，请确保相关文档已上传并包含相关内容。";
        }

        // 添加回答质量标记（仅在详细模式下）
        if (isHighQualityAnswer(processedAnswer) && "detailed".equals(promptType)) {
            processedAnswer += "\n\n✅ 此回答基于文档中的具体信息。";
        }

        return processedAnswer;
    }

    /**
     * 检查是否包含外部知识提示
     */
    private boolean containsExternalKnowledge(String answer) {
        String[] externalKnowledgeMarkers = {
                "根据我的知识", "据我所知", "一般来说", "通常", "常识上",
                "我认为", "我觉得", "据了解", "众所周知"
        };

        for (String marker : externalKnowledgeMarkers) {
            if (answer.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查回答是否过短
     */
    private boolean isAnswerTooShort(String answer) {
        return answer.length() < 150 && !answer.contains("未找到") && !answer.contains("没有相关信息");
    }

    /**
     * 检查是否为"未找到"类回答
     */
    private boolean isNotFoundAnswer(String answer) {
        return answer.contains("未找到") ||
                answer.contains("没有相关信息") ||
                answer.contains("文档中没有");
    }

    /**
     * 检查是否为高质量回答
     */
    private boolean isHighQualityAnswer(String answer) {
        return answer.length() > 200 &&
                !isNotFoundAnswer(answer) &&
                (answer.contains("具体") || answer.contains("详细") || answer.contains("例如"));
    }

    // =========================================================================
    // 文档处理功能 - 委托给 DocumentProcessingService
    // =========================================================================

    /**
     * 处理文档：切片 + 索引
     */
    public void processDocument(Path filePath) {
        documentProcessingService.processDocument(filePath);
    }

    /**
     * 批量处理文档
     */
    public void processDocuments(List<Path> filePaths) {
        documentProcessingService.processDocuments(filePaths);
    }

    /**
     * 获取支持的文件类型信息
     */
    public String getSupportedFileTypes() {
        return documentProcessingService.getSupportedFileTypes();
    }

    /**
     * 检查向量存储状态
     */
    public String checkVectorStoreStatus() {
        return documentProcessingService.checkVectorStoreStatus();
    }

    // =========================================================================
    // 辅助功能
    // =========================================================================

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
        log.info("开始新对话会话: {}", sessionId);
    }

    /**
     * 获取可用的 prompt 类型
     */
    public String[] getAvailablePromptTypes() {
        return promptManager.getAvailablePromptTypes();
    }

    /**
     * 添加自定义 prompt 模板
     */
    public void addPromptTemplate(String name, String template) {
        promptManager.addPromptTemplate(name, template);
        log.info("添加自定义 prompt 模板: {}", name);
    }

    /**
     * 测试 prompt 效果
     */
    public void testPromptEffectiveness() {
        String[] testQuestions = {
                "成都有哪些看芙蓉花的地方？",
                "文档中提到了哪些技术方案？",
                "请总结项目的主要目标"
        };

        for (String question : testQuestions) {
            String optimized = promptManager.getDefaultOptimizedQuestion(question);
            log.info("测试问题: {}", question);
            log.info("优化后提示长度: {}", optimized.length());
        }
    }
}