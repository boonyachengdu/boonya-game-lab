// mcp/McpServer.java
package com.boonya.game.langchain4j.dashscope.mcp;

import com.boonya.game.langchain4j.dashscope.rag.RAGService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class McpServer extends TextWebSocketHandler {

    private final RAGService ragService;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToClient = new ConcurrentHashMap<>();

    public McpServer(RAGService ragService, ObjectMapper objectMapper) {
        this.ragService = ragService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("MCP Client connected: {}", sessionId);

        // 发送初始化响应
        sendInitializeResponse(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.debug("Received MCP message: {}", payload);

            JsonNode request = objectMapper.readTree(payload);
            String method = request.get("method").asText();
            JsonNode id = request.get("id");

            switch (method) {
                case "initialize":
                    handleInitialize(session, request, id);
                    break;
                case "tools/call":
                    handleToolCall(session, request, id);
                    break;
                case "tools/list":
                    handleToolsList(session, id);
                    break;
                case "prompts/list":
                    handlePromptsList(session, id);
                    break;
                case "prompts/get":
                    handlePromptGet(session, request, id);
                    break;
                case "ping":
                    handlePing(session, id);
                    break;
                default:
                    log.warn("Unknown MCP method: {}", method);
                    sendError(session, id, -32601, "Method not found");
            }
        } catch (Exception e) {
            log.error("Error handling MCP message", e);
            sendError(session, null, -32700, "Parse error: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        sessionToClient.values().remove(sessionId);
        log.info("MCP Client disconnected: {}, status: {}", sessionId, status);
    }

    private void sendInitializeResponse(WebSocketSession session) throws IOException {
        Map<String, Object> response = Map.of(
                "jsonrpc", "2.0",
                "id", 1,
                "result", Map.of(
                        "protocolVersion", "2024-11-05",
                        "capabilities", Map.of(
                                "roots", Map.of("listChanged", true),
                                "tools", Map.of(
                                        "listChanged", true
                                ),
                                "prompts", Map.of(
                                        "listChanged", true
                                ),
                                "logging", Map.of(
                                        "logMessage", true
                                )
                        ),
                        "serverInfo", Map.of(
                                "name", "BoonyaRAG-MCP",
                                "version", "1.0.0"
                        )
                )
        );

        sendMessage(session, response);
    }

    private void handleInitialize(WebSocketSession session, JsonNode request, JsonNode id) throws IOException {
        JsonNode params = request.get("params");
        String clientName = params.get("clientInfo").get("name").asText();

        sessionToClient.put(session.getId(), clientName);
        log.info("MCP Client initialized: {}", clientName);

        // 发送 initialized 通知
        Map<String, Object> notification = Map.of(
                "jsonrpc", "2.0",
                "method", "initialized",
                "params", Collections.emptyMap()
        );

        sendMessage(session, notification);
    }

    private void handleToolCall(WebSocketSession session, JsonNode request, JsonNode id) throws IOException {
        JsonNode params = request.get("params");
        String name = params.get("name").asText();
        JsonNode arguments = params.get("arguments");

        try {
            Object result = switch (name) {
                case "ask_question" -> {
                    String sessionId = arguments.has("sessionId") ?
                            arguments.get("sessionId").asText() : UUID.randomUUID().toString();
                    String question = arguments.get("question").asText();
                    String promptType = arguments.has("promptType") ?
                            arguments.get("promptType").asText() : "default";

                    yield Map.of(
                            "content", List.of(
                                    Map.of(
                                            "type", "text",
                                            "text", ragService.askQuestion(sessionId, question, promptType)
                                    )
                            )
                    );
                }
                case "upload_document" -> {
                    // 注意：MCP 通常通过 resources/read 处理文件
                    yield Map.of(
                            "content", List.of(
                                    Map.of(
                                            "type", "text",
                                            "text", "Please use resources/read to provide document content"
                                    )
                            )
                    );
                }
                case "get_conversation_history" -> {
                    String sessionId = arguments.get("sessionId").asText();
                    String history = ragService.getConversationHistory(sessionId);
                    yield Map.of(
                            "content", List.of(
                                    Map.of(
                                            "type", "text",
                                            "text", history
                                    )
                            )
                    );
                }
                case "start_new_conversation" -> {
                    String sessionId = arguments.has("sessionId") ?
                            arguments.get("sessionId").asText() : UUID.randomUUID().toString();
                    ragService.startNewConversation(sessionId);
                    yield Map.of(
                            "content", List.of(
                                    Map.of(
                                            "type", "text",
                                            "text", "Started new conversation with session: " + sessionId
                                    )
                            )
                    );
                }
                default -> throw new IllegalArgumentException("Unknown tool: " + name);
            };

            Map<String, Object> response = Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", result
            );

            sendMessage(session, response);

        } catch (Exception e) {
            log.error("Tool call failed: {}", name, e);
            sendError(session, id, -32000, "Tool execution failed: " + e.getMessage());
        }
    }

    private void handleToolsList(WebSocketSession session, JsonNode id) throws IOException {
        List<Map<String, Object>> tools = List.of(
                Map.of(
                        "name", "ask_question",
                        "description", "Ask a question to the RAG system based on uploaded documents",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "question", Map.of(
                                                "type", "string",
                                                "description", "The question to ask"
                                        ),
                                        "sessionId", Map.of(
                                                "type", "string",
                                                "description", "Conversation session ID (optional, will create new if not provided)"
                                        ),
                                        "promptType", Map.of(
                                                "type", "string",
                                                "enum", List.of("default", "detailed", "concise"),
                                                "description", "Type of prompt to use"
                                        )
                                ),
                                "required", List.of("question")
                        )
                ),
                Map.of(
                        "name", "get_conversation_history",
                        "description", "Get the conversation history for a session",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "sessionId", Map.of(
                                                "type", "string",
                                                "description", "Conversation session ID"
                                        )
                                ),
                                "required", List.of("sessionId")
                        )
                ),
                Map.of(
                        "name", "start_new_conversation",
                        "description", "Start a new conversation session",
                        "inputSchema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "sessionId", Map.of(
                                                "type", "string",
                                                "description", "Session ID (optional, will generate if not provided)"
                                        )
                                )
                        )
                )
        );

        Map<String, Object> response = Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "result", Map.of("tools", tools)
        );

        sendMessage(session, response);
    }

    private void handlePromptsList(WebSocketSession session, JsonNode id) throws IOException {
        List<Map<String, Object>> prompts = List.of(
                Map.of(
                        "name", "rag_question",
                        "description", "Ask a detailed question to the RAG system",
                        "arguments", List.of(
                                Map.of(
                                        "name", "question",
                                        "description", "The detailed question to ask",
                                        "required", true
                                )
                        )
                )
        );

        Map<String, Object> response = Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "result", Map.of("prompts", prompts)
        );

        sendMessage(session, response);
    }

    private void handlePromptGet(WebSocketSession session, JsonNode request, JsonNode id) throws IOException {
        JsonNode params = request.get("params");
        String name = params.get("name").asText();

        if ("rag_question".equals(name)) {
            Map<String, Object> prompt = Map.of(
                    "description", "Ask a detailed question to the RAG system",
                    "arguments", List.of(
                            Map.of(
                                    "name", "question",
                                    "description", "The detailed question to ask",
                                    "required", true
                            )
                    )
            );

            Map<String, Object> response = Map.of(
                    "jsonrpc", "2.0",
                    "id", id,
                    "result", Map.of("prompts", List.of(prompt))
            );

            sendMessage(session, response);
        } else {
            sendError(session, id, -32602, "Prompt not found: " + name);
        }
    }

    private void handlePing(WebSocketSession session, JsonNode id) throws IOException {
        Map<String, Object> response = Map.of(
                "jsonrpc", "2.0",
                "id", id,
                "result", Collections.emptyMap()
        );

        sendMessage(session, response);
    }

    private void sendMessage(WebSocketSession session, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
            log.debug("Sent MCP message: {}", json);
        } catch (Exception e) {
            log.error("Error sending MCP message", e);
        }
    }

    private void sendError(WebSocketSession session, JsonNode id, int code, String message) {
        Map<String, Object> errorResponse = Map.of(
                "jsonrpc", "2.0",
                "id", id != null ? id : JSONNull.getInstance(),
                "error", Map.of(
                        "code", code,
                        "message", message
                )
        );

        sendMessage(session, errorResponse);
    }

    // JSON null 表示
    private static class JSONNull {
        private static final JSONNull INSTANCE = new JSONNull();

        public static JSONNull getInstance() {
            return INSTANCE;
        }
    }
}