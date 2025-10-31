package com.boonya.game.langchain4j.dashscope.document;

// DocumentProcessingService.java
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
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
     * 处理文档：切片 + 索引 - 修复字符编码版本
     */
    public void processDocument(Path filePath) {
        log.info("开始处理文档: {}", filePath.getFileName());

        try {
            // 1. 根据文件类型选择解析策略
            String fileName = filePath.getFileName().toString().toLowerCase();
            Document document;

            if (fileName.endsWith(".pdf")) {
                // 对于PDF，使用增强的文本提取方法
                document = processPdfWithEncodingFix(filePath);
            } else {
                // 其他文件类型使用标准解析
                document = processStandardDocument(filePath, fileName);
            }

            // 2. 验证文档内容
            if (document.text() == null || document.text().trim().isEmpty()) {
                throw new RuntimeException("文档内容为空或无法解析");
            }

            log.info("文档加载成功，内容长度: {}", document.text().length());

            // 3. 文档切片（优化中文处理）
            List<TextSegment> segments = splitDocumentWithChineseOptimization(document);

            // 4. 向量化并存储到向量数据库
            indexSegments(segments);

            log.info("✅ 文档处理完成，生成 {} 个片段", segments.size());

        } catch (Exception e) {
            log.error("❌ 文档处理失败: {}", filePath, e);
            throw new RuntimeException("文档处理失败: " + filePath + " - " + e.getMessage(), e);
        }
    }

    /**
     * 处理PDF文档 - 修复字符编码问题
     */
    private Document processPdfWithEncodingFix(Path pdfPath) {
        log.info("使用增强PDF处理方案处理: {}", pdfPath.getFileName());

        try {
            // 方法1: 首先尝试使用ApachePdfBoxDocumentParser
            try {
                DocumentParser pdfParser = new ApachePdfBoxDocumentParser();
                try (InputStream inputStream = Files.newInputStream(pdfPath)) {
                    Document document = pdfParser.parse(inputStream);
                    String text = cleanPdfText(document.text());
                    if (!text.trim().isEmpty() && containsChinese(text)) {
                        log.info("✅ PDF解析成功（方法1），包含中文字符");
                        return Document.from(text);
                    }
                }
            } catch (Exception e) {
                log.warn("PDF解析方法1失败: {}", e.getMessage());
            }

            // 方法2: 直接使用PDFBox进行文本提取（备用方案）
            try {
                String pdfText = extractTextWithPdfBox(pdfPath);
                if (!pdfText.trim().isEmpty() && containsChinese(pdfText)) {
                    log.info("✅ PDF解析成功（方法2），包含中文字符");
                    return Document.from(pdfText);
                }
            } catch (Exception e) {
                log.warn("PDF解析方法2失败: {}", e.getMessage());
            }

            // 方法3: 使用文本解析器作为最后手段
            try {
                DocumentParser textParser = new TextDocumentParser(StandardCharsets.UTF_8);
                try (InputStream inputStream = Files.newInputStream(pdfPath)) {
                    Document document = textParser.parse(inputStream);
                    String text = cleanPdfText(document.text());
                    if (!text.trim().isEmpty()) {
                        log.warn("⚠️ PDF解析方法3完成，但可能包含乱码");
                        return Document.from(text);
                    }
                }
            } catch (Exception e) {
                log.warn("PDF解析方法3失败: {}", e.getMessage());
            }

            throw new RuntimeException("所有PDF解析方法都失败");

        } catch (Exception e) {
            throw new RuntimeException("PDF处理失败: " + e.getMessage(), e);
        }
    }

    /**
     * 直接使用PDFBox提取文本（字符编码修复）
     */
    private String extractTextWithPdfBox(Path pdfPath) {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();

            // 配置PDF文本提取参数
            stripper.setSortByPosition(true); // 按位置排序
            stripper.setShouldSeparateByBeads(true); // 按段落分离
            stripper.setLineSeparator("\n"); // 使用Unix换行符
            stripper.setParagraphStart("\n"); // 段落开始
            stripper.setParagraphEnd("\n"); // 段落结束
            stripper.setPageStart(""); // 移除页面开始标记
            stripper.setPageEnd(""); // 移除页面结束标记

            String text = stripper.getText(document);
            log.info("PDFBox提取文本成功，页面数: {}, 原始文本长度: {}",
                    document.getNumberOfPages(), text.length());

            return cleanPdfText(text);

        } catch (Exception e) {
            throw new RuntimeException("PDFBox文本提取失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理PDF文本 - 集中处理乱码问题
     */
    private String cleanPdfText(String pdfText) {
        if (pdfText == null || pdfText.trim().isEmpty()) {
            return "";
        }

        // 记录原始文本用于调试
        log.debug("原始PDF文本前100字符: {}",
                pdfText.length() > 100 ? pdfText.substring(0, 100) : pdfText);

        String cleaned = pdfText;

        // 1. 处理换行符和空白字符
        cleaned = cleaned
                .replaceAll("\\r\\n", "\n") // 统一换行符
                .replaceAll("\\r", "\n")
                .replaceAll("\\f", "\n")    // 换页符转行
                .replaceAll("\\n{3,}", "\n\n") // 合并多个空行
                .replaceAll("\\s+", " ")    // 合并多个空白
                .replaceAll("\\u00A0", " ") // 替换不间断空格
                .replaceAll("\\u200B", ""); // 移除零宽空格

        // 2. 处理PDF特有的乱码字符
        cleaned = cleaned
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "") // 移除控制字符
                .replaceAll("�", "") // 移除Unicode替换字符
                .replaceAll("[\\uFFFD]", "") // 移除Unicode替换字符另一种形式
                .replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]", ""); // 更多控制字符

        // 3. 中文文本特殊处理
        cleaned = cleaned
                // 保留中文字符、中文标点、字母、数字、常见标点和空格
                .replaceAll("[^\\u4e00-\\u9fa5\\u3000-\\u303f\\uff00-\\uffef\\w\\s\\p{P}\\n]", "")
                // 优化中文标点周围的空格
                .replaceAll("\\s*([。，；！？])\\s*", "$1")
                // 移除中文之间的多余空格
                .replaceAll("(?<=[\\u4e00-\\u9fa5])\\s+(?=[\\u4e00-\\u9fa5])", "");

        // 4. 最终修剪
        cleaned = cleaned.trim();

        log.debug("PDF文本清理完成: 原始长度={}, 清理后长度={}",
                pdfText.length(), cleaned.length());

        if (cleaned.length() < pdfText.length() * 0.1) {
            log.warn("⚠️ 文本清理后长度显著减少，可能丢失内容");
        }

        return cleaned;
    }

    /**
     * 处理标准文档（非PDF）
     */
    private Document processStandardDocument(Path filePath, String fileName) {
        try {
            DocumentParser parser;

            if (fileName.endsWith(".docx") || fileName.endsWith(".doc")) {
                parser = new ApachePoiDocumentParser();
                log.info("使用Word解析器处理: {}", fileName);
            } else if (fileName.endsWith(".txt")) {
                parser = new TextDocumentParser(StandardCharsets.UTF_8);
                log.info("使用文本解析器处理: {}", fileName);
            } else {
                parser = new TextDocumentParser(StandardCharsets.UTF_8);
                log.warn("未知文件类型，使用文本解析器处理: {}", fileName);
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
    private String cleanStandardText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        return text
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 文档切片 - 针对中文优化
     */
    private List<TextSegment> splitDocumentWithChineseOptimization(Document document) {
        // 针对中文文档优化分割参数
        // 中文通常需要较小的块大小，因为信息密度更高
        DocumentSplitter splitter = DocumentSplitters.recursive(
                400,  // 针对中文优化的块大小（字符数）
                40,   // 重叠大小
                null  // 使用默认的文本分段构建器
        );

        List<TextSegment> segments = splitter.split(document);

        // 后处理：清理每个片段并添加中文优化
        List<TextSegment> cleanedSegments = new ArrayList<>();
        for (TextSegment segment : segments) {
            String cleanedText = cleanSegmentText(segment.text());
            if (!cleanedText.trim().isEmpty() && cleanedText.length() > 10) {
                TextSegment cleanedSegment = TextSegment.from(cleanedText, segment.metadata());
                cleanedSegments.add(cleanedSegment);
            }
        }

        log.debug("文档分割完成: 原始片段数={}, 清理后片段数={}",
                segments.size(), cleanedSegments.size());

        return cleanedSegments;
    }

    /**
     * 清理片段文本
     */
    private String cleanSegmentText(String segmentText) {
        if (segmentText == null || segmentText.trim().isEmpty()) {
            return "";
        }

        // 移除片段级别的乱码
        String cleaned = segmentText
                .replaceAll("[^\\u4e00-\\u9fa5\\u3000-\\u303f\\uff00-\\uffef\\w\\s\\p{P}\\n]", "")
                .replaceAll("\\s+", " ")
                .trim();

        return cleaned.isEmpty() ? "[空内容]" : cleaned;
    }

    /**
     * 索引片段到向量数据库
     */
    private void indexSegments(List<TextSegment> segments) {
        if (segments.isEmpty()) {
            log.warn("没有可索引的片段");
            return;
        }

       /* EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ingestor.ingest((Document) segments);*/

        // 向量化并存储到向量数据库
        List<dev.langchain4j.data.embedding.Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);

        log.info("成功索引 {} 个片段到向量数据库", segments.size());
    }

    /**
     * 检查文本是否包含中文字符
     */
    private boolean containsChinese(String text) {
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
}