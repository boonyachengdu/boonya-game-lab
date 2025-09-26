package com.boonya.game.ai.rag.advanced.service;

import com.boonya.game.ai.rag.base.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 缓存与性能优化
 */
@Service
public class CachedRagService {

    @Autowired
    private RagService ragService;

    // 使用Redis缓存常见问题的答案
    @Cacheable(value = "ragAnswers", key = "#question.hashCode()")
    public String getCachedAnswer(String question) {
        return ragService.generateAnswer(question);
    }
}