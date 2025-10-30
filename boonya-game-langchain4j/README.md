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
