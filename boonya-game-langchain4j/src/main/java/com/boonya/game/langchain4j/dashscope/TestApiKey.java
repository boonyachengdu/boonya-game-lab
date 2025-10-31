package com.boonya.game.langchain4j.dashscope;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;

import java.util.List;

public class TestApiKey {
    public static void main(String[] args) {
        String apiKey = "xxxxxx";
        Generation gen = new Generation();
        Message message = Message.builder()
                .role("user")
                .content("帮我查询成都明天的天气")
                .build();
        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(Generation.Models.QWEN_TURBO)
                .messages(List.of(message))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
        try {
            GenerationResult response = gen.call(param);
            System.out.println(response.getOutput().getChoices().get(0).getMessage().getContent());
        } catch (NoApiKeyException e) {
            System.out.println("API密钥错误: " + e.getMessage());
        } catch (InputRequiredException e) {
            System.out.println("输入参数错误: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("其他错误: " + e.getMessage());
        }
    }
}