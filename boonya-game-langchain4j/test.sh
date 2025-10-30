# 上传文档
curl -X POST "http://localhost:8080/api/rag/upload?filePath=/path/to/your/document.txt"

# 提问
curl -X POST "http://localhost:8080/api/rag/ask?sessionId=user123&question=文档中提到了哪些关键技术？"

# 查看对话历史
curl "http://localhost:8080/api/rag/history/user123"

# 开始新对话
curl -X POST "http://localhost:8080/api/rag/new-session/user123"