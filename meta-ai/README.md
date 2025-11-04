# Langchain4j



# chroma本地安装

## Python 环境安装

```
# 创建 Python 虚拟环境
python -m venv chroma_env
source chroma_env/bin/activate  # Linux/Mac
# 或者 chroma_env\Scripts\activate  # Windows

# 安装 Chroma
pip install chromadb

# 安装可选依赖（推荐）
pip install chromadb[all]  # 包含所有功能
# 或者选择特定功能
pip install chromadb[client]  # 仅客户端
pip install chromadb[server]  # 服务器功能
```

## 启动服务
```
# 启动 Chroma 服务器
chroma run --host 0.0.0.0 --port 8000

# 或者作为模块运行
python -m chromadb run --host 0.0.0.0 --port 8000

# 持久化存储
chroma run --host 0.0.0.0 --port 8000 --persist-directory ./chroma_data

```

# Prompt 提示词优化

## 推荐方案：后端统一处理

* 在 RAGService 中统一管理 prompt
* 使用专门的 PromptManager 类
* 设计针对 RAG 的专用 prompt 模板
* 添加回答后处理逻辑

## Prompt 设计要点

* 明确约束：要求只基于文档内容
* 格式要求：规定回答结构和详细程度
* 诚实原则：明确告知信息缺失情况
* 拒绝编造：严禁添加外部知识

## 实施步骤

1. 立即使用基础的 prompt 优化版本
2. 逐步实现 PromptManager 高级功能
3. 根据实际效果调整 prompt 模板
4. 添加回答质量监控

这样优化后，对于"成都有哪些看芙蓉花的地方"这类问题，AI 会：

* 严格基于上传的文档内容回答
* 提供全面详细的信息（如果文档中有）
* 明确告知信息缺失（如果文档中没有）
* 不会编造虚假地点或信息

# RAG 过程

✅ 正确的向量生成：使用 embeddingModel.embedAll(segments)

✅ 正确的向量存储：使用 embeddingStore.addAll(embeddings, segments)

✅ 完善的错误处理：详细的日志和异常处理

✅ 完整的 RAG 流程：文档处理 → 向量化 → 存储 → 检索 → 生成