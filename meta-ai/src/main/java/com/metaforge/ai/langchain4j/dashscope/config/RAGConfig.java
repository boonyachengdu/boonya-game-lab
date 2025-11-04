// RAGConfig
package com.metaforge.ai.langchain4j.dashscope.config;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class RAGConfig {

    @Value("${dashscope.api-key}")
    private String dashscopeApiKey;

    @Value("${dashscope.request:qwen-turbo}")
    private String dashscopeModel;

    @Value("${dashscope.max-retries:3}")
    private int maxRetries;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        log.info("初始化通义千问聊天模型 - 模型: {}, API密钥: {}",
                dashscopeModel, maskApiKey(dashscopeApiKey));

        if (dashscopeApiKey == null || dashscopeApiKey.trim().isEmpty()) {
            throw new RuntimeException("通义千问API密钥未配置");
        }

        try {
            Generation generation = new Generation();
            return new ChatLanguageModel() {
                @Override
                public Response<AiMessage> generate(List<ChatMessage> messages) {
                    int retryCount = 0;
                    Exception lastException = null;

                    while (retryCount <= maxRetries) {
                        try {
                            log.debug("通义千问处理 {} 条消息 (重试: {}/{})",
                                    messages.size(), retryCount, maxRetries);

                            // 验证输入
                            if (messages == null || messages.isEmpty()) {
                                throw new IllegalArgumentException("消息列表不能为空");
                            }

                            // 转换为DashScope的Message列表
                            List<Message> dashScopeMessages = convertToDashScopeMessages(messages);

                            // 构建请求参数
                            GenerationParam param = GenerationParam.builder()
                                    .apiKey(dashscopeApiKey)
                                    .model(dashscopeModel)
                                    .messages(dashScopeMessages)
                                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                                    .build();

                            // 调用生成
                            GenerationResult response = generation.call(param);

                            // 验证响应
                            if (response == null || response.getOutput() == null ||
                                    response.getOutput().getChoices() == null ||
                                    response.getOutput().getChoices().isEmpty()) {
                                throw new RuntimeException("API返回空响应");
                            }

                            String aiText = response.getOutput().getChoices().get(0).getMessage().getContent();

                            if (aiText == null || aiText.trim().isEmpty()) {
                                throw new RuntimeException("AI回复内容为空");
                            }

                            AiMessage aiMessage = new AiMessage(aiText);

                            log.debug("通义千问回复成功，长度: {}", aiText.length());
                            return Response.from(aiMessage);

                        } catch (NoApiKeyException e) {
                            log.error("API密钥错误", e);
                            throw new RuntimeException("通义千问API密钥配置错误: " + e.getMessage(), e);
                        } catch (InputRequiredException e) {
                            log.error("输入参数错误", e);
                            throw new RuntimeException("输入参数不完整: " + e.getMessage(), e);
                        } catch (Exception e) {
                            lastException = e;
                            retryCount++;
                            log.warn("通义千问调用失败，准备重试 {}/{}", retryCount, maxRetries, e);

                            if (retryCount <= maxRetries) {
                                try {
                                    Thread.sleep(1000 * retryCount); // 指数退避
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    throw new RuntimeException("重试被中断", ie);
                                }
                            }
                        }
                    }

                    // 所有重试都失败了
                    log.error("通义千问调用失败，已达到最大重试次数: {}", maxRetries);
                    throw new RuntimeException("通义千问服务调用失败，已重试 " + maxRetries + " 次: " +
                            (lastException != null ? lastException.getMessage() : "未知错误"), lastException);
                }

                @Override
                public String generate(String userMessage) {
                    // 向后兼容的单消息接口
                    log.debug("使用单消息接口生成响应");

                    if (userMessage == null || userMessage.trim().isEmpty()) {
                        throw new IllegalArgumentException("用户消息不能为空");
                    }

                    List<ChatMessage> messages = List.of(UserMessage.from(userMessage));
                    Response<AiMessage> response = generate(messages);
                    return response.content().text();
                }
            };
        } catch (Exception e) {
            log.error("通义千问模型初始化失败", e);
            throw new RuntimeException("无法初始化通义千问模型", e);
        }
    }

    /**
     * 将 LangChain4J 的 ChatMessage 列表转换为 DashScope 的 Message 列表
     */
    private List<Message> convertToDashScopeMessages(List<ChatMessage> chatMessages) {
        List<Message> dashScopeMessages = new ArrayList<>();

        for (ChatMessage chatMessage : chatMessages) {
            String role = getRole(chatMessage);
            String content = chatMessage.text();

            Message message = Message.builder()
                    .role(role)
                    .content(content)
                    .build();

            dashScopeMessages.add(message);

            if (log.isDebugEnabled()) {
                String contentPreview = content.length() > 100 ?
                        content.substring(0, 100) + "..." : content;
                log.debug("转换消息: role={}, content={}", role, contentPreview);
            }
        }

        return dashScopeMessages;
    }

    /**
     * 根据消息类型获取角色
     */
    private String getRole(ChatMessage chatMessage) {
        if (chatMessage instanceof SystemMessage) {
            return "system";
        } else if (chatMessage instanceof UserMessage) {
            return "user";
        } else if (chatMessage instanceof AiMessage) {
            return "assistant";
        } else {
            log.warn("未知消息类型: {}, 默认设置为用户消息", chatMessage.getClass().getSimpleName());
            return "user";
        }
    }

    /**
     * 掩码显示 API 密钥
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() <= 8) {
            return "***";
        }
        return apiKey.substring(0, 4) + "***" + apiKey.substring(apiKey.length() - 4);
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("初始化嵌入模型: AllMiniLmL6V2EmbeddingModel");
        AllMiniLmL6V2EmbeddingModel model = new AllMiniLmL6V2EmbeddingModel();

        try {
            Response<Embedding> testResponse = model.embed("测试文本");
            log.info("嵌入模型测试成功，向量维度: {}", testResponse.content().dimension());
        } catch (Exception e) {
            log.error("嵌入模型测试失败", e);
        }

        return model;
    }
}