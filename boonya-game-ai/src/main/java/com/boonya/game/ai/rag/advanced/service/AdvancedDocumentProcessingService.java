package com.boonya.game.ai.rag.advanced.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 文档预处理流水线
 */
@Service
public class AdvancedDocumentProcessingService {

    @Autowired
    private VectorStore vectorStore;

    // 支持多种文档解析器
    public void processEnterpriseDocument(Resource resource, String filename) {
        List<Document> documents = null;

        // 根据文件类型选择解析器
        if (filename.endsWith(".pdf")) {
            documents = new TikaDocumentReader(resource).get();
        } else if (filename.endsWith(".docx") || filename.endsWith(".doc")) {
            documents = new PagePdfDocumentReader(resource).get();
        } else {
            documents = new TextReader(resource).get();
        }

        // 高级文本切分：按章节/标题分割
        // TODO 文本切分的参数含义需要注意
        TokenTextSplitter splitter = new TokenTextSplitter(1000, 200, 100, 100, true);
        List<Document> chunks = splitter.apply(documents);

        // 为每个块添加元数据，便于溯源
        chunks.forEach(doc -> {
            doc.getMetadata().put("source_file", filename);
            doc.getMetadata().put("ingest_time", Instant.now().toString());
        });

        vectorStore.add(chunks);
    }
}
