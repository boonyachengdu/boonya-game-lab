package com.boonya.game.langchain4j.dashscope.document;

// DocumentProcessingService.java

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

@Service
public class DocumentProcessingService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public DocumentProcessingService(EmbeddingModel embeddingModel,
                                     EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    /**
     * 处理文档：切片 + 索引
     */
    public void processDocument(Path filePath) {
        try {
            // 1. 加载文档
            DocumentParser parser = new TextDocumentParser();
            InputStream inputStream = filePath.toFile().toURI().toURL().openStream();
            Document document = parser.parse(inputStream);

            // 2. 文档切片（使用递归切片器，保持语义完整性）
            DocumentSplitter splitter = DocumentSplitters.recursive(500, 50); // 500字符块，50字符重叠
            List<TextSegment> segments = splitter.split(document);

           /* // 3. 向量化并存储到向量数据库
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .embeddingModel(embeddingModel)
                    .embeddingStore(embeddingStore)
                    .build();

            ingestor.ingest(segments);*/

            // 3. 向量化并存储到向量数据库
            List<dev.langchain4j.data.embedding.Embedding> embeddings = embeddingModel.embedAll(segments).content();
            embeddingStore.addAll(embeddings, segments);

            System.out.println("文档处理完成，生成 " + segments.size() + " 个片段");

        } catch (Exception e) {
            throw new RuntimeException("文档处理失败: " + filePath, e);
        }
    }

    /**
     * 批量处理文档
     */
    public void processDocuments(List<Path> filePaths) {
        filePaths.forEach(this::processDocument);
    }
}