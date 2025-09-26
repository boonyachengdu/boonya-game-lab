package com.boonya.game.ai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 向量存储配置
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate,
                                   EmbeddingModel embeddingModel) {

        PgVectorStore.PgDistanceType distanceType = PgVectorStore.PgDistanceType.COSINE_DISTANCE;

        // 索引构建参数
        PgVectorStore.PgIndexType indexType = PgVectorStore.PgIndexType.HNSW;

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .distanceType(distanceType)
                .indexType(indexType)
                .dimensions(1536)  // 明确指定维度
                .removeExistingVectorStoreTable(true) // 启动时清空表（开发环境）
                .build();
    }
}