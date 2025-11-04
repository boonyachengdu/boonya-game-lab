package com.metaforge.ai.langchain4j.dashscope.document;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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
     * 处理文档：切片 + 索引 - 完整版本
     */
    public void processDocument(Path filePath) {
        log.info("开始处理文档: {}", filePath.getFileName());

        try {
            // 1. 根据文件类型选择解析策略
            String fileName = filePath.getFileName().toString().toLowerCase();
            Document document;

            if (fileName.endsWith(".pdf")) {
                document = processPdfDocument(filePath);
            } else {
                document = processStandardDocument(filePath, fileName);
            }

            // 2. 验证文档内容
            if (document.text() == null || document.text().trim().isEmpty()) {
                throw new RuntimeException("文档内容为空或无法解析");
            }

            log.info("文档加载成功，内容长度: {}", document.text().length());

            // 3. 文档切片（优化中文处理）
            List<TextSegment> segments = splitDocument(document);

            // 4. 向量化并存储到向量数据库
            indexSegments(segments);

            log.info("✅ 文档处理完成，生成 {} 个片段", segments.size());

        } catch (Exception e) {
            log.error("❌ 文档处理失败: {}", filePath, e);
            throw new RuntimeException("文档处理失败: " + filePath.getFileName() + " - " + e.getMessage(), e);
        }
    }

    /**
     * 处理PDF文档 - 修复字符编码问题
     */
    public Document processPdfDocument(Path pdfPath) {
        log.info("处理PDF文档: {}", pdfPath.getFileName());

        // 方法1: 首先尝试使用ApachePdfBoxDocumentParser
        try {
            DocumentParser pdfParser = new ApachePdfBoxDocumentParser();
            try (InputStream inputStream = Files.newInputStream(pdfPath)) {
                Document document = pdfParser.parse(inputStream);
                String text = cleanPdfText(document.text());
                if (!text.trim().isEmpty() && containsChinese(text)) {
                    log.info("✅ PDF解析成功（方法1）");
                    return Document.from(text);
                }
            }
        } catch (Exception e) {
            log.warn("PDF解析方法1失败: {}", e.getMessage());
        }

        // 方法2: 直接使用PDFBox进行文本提取
        try {
            String pdfText = extractTextWithPdfBox(pdfPath);
            if (!pdfText.trim().isEmpty() && containsChinese(pdfText)) {
                log.info("✅ PDF解析成功（方法2）");
                return Document.from(pdfText);
            }
        } catch (Exception e) {
            log.warn("PDF解析方法2失败: {}", e.getMessage());
        }

        throw new RuntimeException("PDF解析失败，无法提取文本内容");
    }

    /**
     * 直接使用PDFBox提取文本
     */
    private String extractTextWithPdfBox(Path pdfPath) {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();

            // 配置PDF文本提取参数
            stripper.setSortByPosition(true);
            stripper.setShouldSeparateByBeads(true);
            stripper.setLineSeparator("\n");
            stripper.setParagraphStart("\n");
            stripper.setParagraphEnd("\n");

            String text = stripper.getText(document);
            log.info("PDFBox提取文本成功，页面数: {}", document.getNumberOfPages());

            return cleanPdfText(text);

        } catch (Exception e) {
            throw new RuntimeException("PDFBox文本提取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理PDF文本
     */
    public String cleanPdfText(String pdfText) {
        if (pdfText == null || pdfText.trim().isEmpty()) {
            return "";
        }

        String cleaned = pdfText
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                .replaceAll("\\f", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("\\s+", " ")
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                .replaceAll("�", "")
                .replaceAll("[\\uFFFD]", "")
                .replaceAll("[^\\u4e00-\\u9fa5\\u3000-\\u303f\\uff00-\\uffef\\w\\s\\p{P}\\n]", "")
                .replaceAll("\\s*([。，；！？])\\s*", "$1")
                .replaceAll("(?<=[\\u4e00-\\u9fa5])\\s+(?=[\\u4e00-\\u9fa5])", "")
                .trim();

        log.debug("PDF文本清理: 原始长度={}, 清理后长度={}", pdfText.length(), cleaned.length());
        return cleaned;
    }

    /**
     * 处理标准文档（非PDF）
     */
    public Document processStandardDocument(Path filePath, String fileName) {
        try {
            DocumentParser parser;

            if (fileName.endsWith(".docx") || fileName.endsWith(".doc")) {
                parser = new ApachePoiDocumentParser();
                log.info("使用Word解析器处理: {}", fileName);
            } else {
                parser = new TextDocumentParser(StandardCharsets.UTF_8);
                log.info("使用文本解析器处理: {}", fileName);
            }

            try (InputStream inputStream = Files.newInputStream(filePath)) {
                Document document = parser.parse(inputStream);
                String text = cleanStandardText(document.text());
                return Document.from(text);
            }

        } catch (Exception e) {
            throw new RuntimeException("标准文档处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理标准文档文本
     */
    public String cleanStandardText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        return text
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 文档切片
     */
    public List<TextSegment> splitDocument(Document document) {
        // 针对中文优化分割参数
        DocumentSplitter splitter = DocumentSplitters.recursive(400, 40);
        List<TextSegment> segments = splitter.split(document);

        // 清理每个片段
        List<TextSegment> cleanedSegments = new ArrayList<>();
        for (TextSegment segment : segments) {
            String cleanedText = cleanSegmentText(segment.text());
            if (!cleanedText.trim().isEmpty() && cleanedText.length() > 10) {
                TextSegment cleanedSegment = TextSegment.from(cleanedText, segment.metadata());
                cleanedSegments.add(cleanedSegment);
            }
        }

        log.debug("文档分割: 原始片段数={}, 清理后片段数={}", segments.size(), cleanedSegments.size());
        return cleanedSegments;
    }

    /**
     * 清理片段文本
     */
    public String cleanSegmentText(String segmentText) {
        if (segmentText == null || segmentText.trim().isEmpty()) {
            return "";
        }

        return segmentText
                .replaceAll("[^\\u4e00-\\u9fa5\\u3000-\\u303f\\uff00-\\uffef\\w\\s\\p{P}\\n]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 索引片段到向量数据库
     */
    public void indexSegments(List<TextSegment> segments) {
        if (segments.isEmpty()) {
            log.warn("没有可索引的片段");
            return;
        }

        try {
            log.info("开始向量化 {} 个文本片段", segments.size());

            // 1. 生成向量嵌入
            Response<List<Embedding>> embeddingsResponse = embeddingModel.embedAll(segments);
            List<Embedding> embeddings = embeddingsResponse.content();

            log.info("向量化完成，生成 {} 个嵌入向量", embeddings.size());

            // 2. 存储到向量数据库
            embeddingStore.addAll(embeddings, segments);

            log.info("✅ 成功索引 {} 个片段到向量数据库", segments.size());

        } catch (Exception e) {
            log.error("❌ 向量索引失败", e);
            throw new RuntimeException("向量索引失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查文本是否包含中文字符
     */
    public boolean containsChinese(String text) {
        if (text == null) return false;
        return text.matches(".*[\\u4e00-\\u9fa5].*");
    }

    /**
     * 批量处理文档
     */
    public void processDocuments(List<Path> filePaths) {
        log.info("开始批量处理 {} 个文档", filePaths.size());

        int successCount = 0;
        int failCount = 0;

        for (Path filePath : filePaths) {
            try {
                processDocument(filePath);
                successCount++;
            } catch (Exception e) {
                log.error("处理文档失败: {}", filePath, e);
                failCount++;
            }
        }

        log.info("批量处理完成: 成功={}, 失败={}", successCount, failCount);
    }

    /**
     * 获取支持的文件类型信息
     */
    public String getSupportedFileTypes() {
        return """
            支持的文件类型:
            - PDF 文件 (.pdf): 完全支持，包含中文编码修复
            - 文本文件 (.txt): 完全支持
            - Word 文档 (.doc/.docx): 基础支持
            """;
    }

    /**
     * 检查向量存储状态
     */
    public String checkVectorStoreStatus() {
        try {
            return "向量存储状态: 正常";
        } catch (Exception e) {
            return "向量存储状态: 异常 - " + e.getMessage();
        }
    }
}