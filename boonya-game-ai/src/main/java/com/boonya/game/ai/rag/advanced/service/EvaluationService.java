package com.boonya.game.ai.rag.advanced.service;

import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EvaluationService {

    public EvaluationResponse evaluate(EvaluationRequest request) {
        // TODO 评估逻辑
        return new EvaluationResponse(true, "评估结果", Map.of());
    }
}
