// McpConfig.java
package com.metaforge.ai.langchain4j.dashscope.config;

import com.metaforge.ai.langchain4j.dashscope.mcp.McpServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class McpConfig implements WebSocketConfigurer {

    private final McpServer mcpServer;

    public McpConfig(McpServer mcpServer) {
        this.mcpServer = mcpServer;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(mcpServer, "/mcpWebsocket")
                .setAllowedOriginPatterns("*");
    }
}