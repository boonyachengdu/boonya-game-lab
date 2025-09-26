package com.boonya.game.ai.rag.advanced.controller;

import com.boonya.game.ai.rag.advanced.service.EvaluationService;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
// 实现评估端点（用于评估检索质量）
@RestController
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    @PostMapping("/admin/evaluate")
    public EvaluationResponse evaluateRetrieval(@RequestBody EvaluationRequest request) {
        // 人工标注的标准答案
        // 调用RAG系统生成答案
        // 使用BLEU、ROUGE等指标自动评估，或提供界面供人工评估
        return evaluationService.evaluate(request);
    }
}
