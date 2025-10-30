package com.boonya.game.langchain4j.dashscope.rag;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

// 定义AI助手接口
public interface Assistant {

    @SystemMessage("""
        你是一个专业的文档助手。请基于提供的文档内容回答问题。
        如果文档中没有相关信息，请明确说明。
        请用中文回复，保持专业和准确。
        """)
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);
}

