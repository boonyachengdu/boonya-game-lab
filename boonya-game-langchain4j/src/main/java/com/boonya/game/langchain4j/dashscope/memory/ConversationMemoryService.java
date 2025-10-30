package com.boonya.game.langchain4j.dashscope.memory;

// ConversationMemoryService.java
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationMemoryService {

    private final Map<String, ChatMemory> chatMemories = new ConcurrentHashMap<>();
    private static final int MAX_MESSAGES = 10; // 保存最近10轮对话

    /**
     * 获取或创建对话记忆
     */
    public ChatMemory getOrCreateMemory(String sessionId) {
        return chatMemories.computeIfAbsent(sessionId,
                id -> MessageWindowChatMemory.withMaxMessages(MAX_MESSAGES));
    }

    /**
     * 添加用户消息到记忆
     */
    public void addUserMessage(String sessionId, String message) {
        ChatMemory memory = getOrCreateMemory(sessionId);
        memory.add(UserMessage.from(message));
    }

    /**
     * 添加AI回复到记忆
     */
    public void addAiMessage(String sessionId, String message) {
        ChatMemory memory = getOrCreateMemory(sessionId);
        memory.add(AiMessage.from(message));
    }

    /**
     * 清除对话记忆
     */
    public void clearMemory(String sessionId) {
        chatMemories.remove(sessionId);
    }

    /**
     * 获取对话历史
     */
    public String getConversationHistory(String sessionId) {
        ChatMemory memory = chatMemories.get(sessionId);
        if (memory == null) {
            return "无历史对话";
        }
        return memory.messages().toString();
    }

    public ChatMemory getOrCreateMemory(Object sessionId) {
        return getOrCreateMemory(sessionId.toString());
    }
}
