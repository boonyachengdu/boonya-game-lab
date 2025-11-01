// mcp/test/McpClientTest.java
package com.boonya.game.langchain4j.dashscope.mcp;

import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
public class McpClientTest {

    @Test
    public void testMcpConnection() throws Exception {
        CompletableFuture<String> responseFuture = new CompletableFuture<>();

        WebSocketClient client = new WebSocketClient(URI.create("ws://localhost:8080/mcp")) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                log.info("Connected to MCP server");

                // 发送初始化请求
                JSONObject initRequest = new JSONObject();
                try {
                    initRequest.put("jsonrpc", "2.0");
                    initRequest.put("id", 1);
                    initRequest.put("method", "initialize");
                    initRequest.put("params", new JSONObject()
                            .put("protocolVersion", "2024-11-05")
                            .put("capabilities", new JSONObject())
                            .put("clientInfo", new JSONObject()
                                    .put("name", "test-client")
                                    .put("version", "1.0.0")));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                this.send(initRequest.toString());
            }

            @Override
            public void onMessage(String message) {
                log.info("Received: {}", message);
                responseFuture.complete(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                log.info("Connection closed: {} - {}", code, reason);
            }

            @Override
            public void onError(Exception ex) {
                log.error("WebSocket error", ex);
                responseFuture.completeExceptionally(ex);
            }
        };

        client.connect();

        // 等待响应
        String response = responseFuture.get(10, TimeUnit.SECONDS);
        log.info("Test completed with response: {}", response);
    }
}