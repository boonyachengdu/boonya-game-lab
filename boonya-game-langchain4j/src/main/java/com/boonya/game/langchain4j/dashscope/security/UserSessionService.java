package com.boonya.game.langchain4j.dashscope.security;

// UserSessionService.java
import com.boonya.game.langchain4j.dashscope.memory.ConversationMemoryService;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserSessionService {

    private final Map<String, UserSession> userSessions = new ConcurrentHashMap<>();
    private final ConversationMemoryService memoryService;

    public UserSessionService(ConversationMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * 创建用户会话
     */
    public UserSession createSession(String username) {
        String sessionId = generateSessionId();
        UserSession session = new UserSession(sessionId, username);
        userSessions.put(sessionId, session);
        return session;
    }

    /**
     * 获取用户会话
     */
    public UserSession getSession(String sessionId) {
        return userSessions.get(sessionId);
    }

    /**
     * 更新用户会话活动时间
     */
    public void updateSessionActivity(String sessionId) {
        UserSession session = userSessions.get(sessionId);
        if (session != null) {
            session.updateLastActivity();
        }
    }

    /**
     * 结束会话
     */
    public void endSession(String sessionId) {
        // 清理对话记忆
        memoryService.clearMemory(sessionId);
        userSessions.remove(sessionId);
    }

    /**
     * 获取用户的所有会话
     */
    public Map<String, UserSession> getUserSessions(String username) {
        return userSessions.entrySet().stream()
                .filter(entry -> entry.getValue().getUsername().equals(username))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * 清理过期会话（可定时调用）
     */
    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        userSessions.entrySet().removeIf(entry ->
                now - entry.getValue().getLastActivityTime() > 30 * 60 * 1000 // 30分钟过期
        );
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    public static class UserSession {
        private final String sessionId;
        private final String username;
        private long createTime;
        private long lastActivityTime;
        private int messageCount;

        public UserSession(String sessionId, String username) {
            this.sessionId = sessionId;
            this.username = username;
            this.createTime = System.currentTimeMillis();
            this.lastActivityTime = this.createTime;
            this.messageCount = 0;
        }

        public void updateLastActivity() {
            this.lastActivityTime = System.currentTimeMillis();
        }

        public void incrementMessageCount() {
            this.messageCount++;
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public String getUsername() { return username; }
        public long getCreateTime() { return createTime; }
        public long getLastActivityTime() { return lastActivityTime; }
        public int getMessageCount() { return messageCount; }
    }
}
