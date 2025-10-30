package com.boonya.game.langchain4j.dashscope.vector;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Data
@ConditionalOnProperty(name = "vector.store", havingValue = "chroma")
@Configuration
@ConfigurationProperties(prefix = "chroma")
@Slf4j
public class ChromaConfig {
    private String url;
    private String collection;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        log.info("正在初始化 Chroma 向量数据库...");

        // 首先测试 Chroma 连接
        if (!testChromaAvailability()) {
            log.error("❌ Chroma 服务不可用，使用内存存储");
            return new InMemoryEmbeddingStore<>();
        }

        try {
            // 方案 1: 标准配置
            return createStandardChromaStore();

        } catch (Exception e1) {
            log.warn("标准配置失败，尝试备用配置: {}", e1.getMessage());

            try {
                // 方案 2: 备用配置ss
                return createFallbackChromaStore();

            } catch (Exception e2) {
                log.error("所有 Chroma 配置都失败，使用内存存储: {}", e2.getMessage());
                return new InMemoryEmbeddingStore<>();
            }
        }
    }

    private boolean testChromaAvailability() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url + "/api/v2/heartbeat", String.class);
            log.info("✅ Chroma 服务可用: {}", response);
            return true;
        } catch (Exception e) {
            log.error("❌ Chroma 服务不可用: {}", e.getMessage());
            return false;
        }
    }

    private EmbeddingStore<TextSegment> createStandardChromaStore() {
        log.info("尝试标准 Chroma 配置...");

        ChromaEmbeddingStore store = ChromaEmbeddingStore.builder()
                .timeout(Duration.ofMillis(15000))
                .baseUrl(url)
                .collectionName(collection)
                .build();

        log.info("✅ 标准 Chroma 配置成功");
        return store;
    }

    private EmbeddingStore<TextSegment> createFallbackChromaStore() {
        log.info("尝试备用 Chroma 配置...");

        // 配置更长的超时时间
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(30000);
        requestFactory.setReadTimeout(30000);

        RestTemplate restTemplate = new RestTemplate(requestFactory);

        // 这里需要根据实际的 ChromaEmbeddingStore 实现来调整
        // 有些版本可能不支持直接设置 RestTemplate

        ChromaEmbeddingStore store = ChromaEmbeddingStore.builder()
                .baseUrl(url)
                .collectionName(collection)
                .build();

        log.info("✅ 备用 Chroma 配置成功");
        return store;
    }
}