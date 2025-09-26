package com.boonya.game.ai.rag.base.controller;

import com.boonya.game.ai.rag.base.service.DocumentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/ingest")
    public String ingestDocument(@RequestParam("file") MultipartFile file) {
        try {
            documentService.ingestDocument(file);
            return "文档入库成功！";
        } catch (Exception e) {
            return "文档入库失败: " + e.getMessage();
        }
    }
}
