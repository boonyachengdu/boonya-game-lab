package com.boonya.game.langchain4j.dashscope.vector;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@ConditionalOnProperty(name = "vector.store", havingValue = "milvus")
@ConfigurationProperties(prefix = "milvus")
@Configuration
public class MilvusConfig {

    private String host;
    private Integer port;
    private String collection;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return MilvusEmbeddingStore.builder()
                .host(host)
                .port(port)
                .collectionName(collection)
                .dimension(384) // 根据嵌入模型维度调整
                .build();
    }
}
