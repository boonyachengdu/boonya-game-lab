package com.boonya.game.langchain4j.dashscope.config;

// RAGConfig.java

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class RAGConfig {

    @Value("${dashscope.api-key}")
    private String DASHSCOPE_API_KEY;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        // 配置通义千问模型
        return new ChatLanguageModel() {
            private final Generation gen = new Generation();

            @Override
            public String generate(String userMessage) {
                try {
                    GenerationParam param = GenerationParam.builder()
                            .apiKey(DASHSCOPE_API_KEY)
                            .model(Generation.Models.QWEN_TURBO)
                            .prompt(userMessage)
                            .build();
                    return gen.call(param).getOutput().getText();
                } catch (Exception e) {
                    throw new RuntimeException("调用通义千问失败", e);
                }
            }

            @Override
            public Response<AiMessage> generate(List<ChatMessage> list) {
                // TODO 此处实现多个聊天消息的生成
                throw new RuntimeException("未实现多个聊天消息的生成");
            }
        };
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        // 使用轻量级嵌入模型
        return new AllMiniLmL6V2EmbeddingModel();
    }
}
