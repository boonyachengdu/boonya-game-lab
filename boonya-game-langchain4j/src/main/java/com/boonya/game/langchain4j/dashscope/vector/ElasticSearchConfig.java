package com.boonya.game.langchain4j.dashscope.vector;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@ConditionalOnProperty(name = "vector.store", havingValue = "elasticsearch")
@ConfigurationProperties(prefix = "elasticsearch")
@Configuration
public class ElasticSearchConfig {

    private String url;
    private String index;

    @Bean
    public EmbeddingStore<TextSegment> elasticsearchEmbeddingStore() {
        return ElasticsearchEmbeddingStore.builder()
                .serverUrl(url)
                .indexName(index)
                .build();
    }
}
