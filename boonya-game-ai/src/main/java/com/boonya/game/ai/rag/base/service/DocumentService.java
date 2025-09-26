package com.boonya.game.ai.rag.base.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class DocumentService {

    private final VectorStore vectorStore;

    public DocumentService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * 处理上传的文本文件，将其内容存入向量库
     */
    public void ingestDocument(MultipartFile file) throws IOException {
        // 1. 将文件转换为 Spring AI 的 Resource
        Resource resource = file.getResource();

        // 2. 使用文本阅读器解析文件
        TextReader textReader = new TextReader(resource);
        textReader.getCustomMetadata().put("filename", file.getOriginalFilename());
        List<Document> documents = textReader.get();

        // 3. 文本切分（防止文本过长，超出模型上下文限制）
        // TODO 添加自定义切分逻辑，参数含义要搞清楚
        TokenTextSplitter textSplitter = new TokenTextSplitter(500, 100,100,100,true); // chunk size=500 tokens, overlap=100 tokens
        List<Document> splitDocuments = textSplitter.apply(documents);

        // 4. 将切分后的文档存入向量库
        vectorStore.add(splitDocuments);
    }
}
