# chroma
docker run  -d --name chroma -p 8000:8000 chromadb/chroma
# chroma-ui
docker run  -d --name chroma-ui -p 8001:8001 chromadb/chroma-ui


# milvus
docker run -d --name milvus_cpu -p 19530:19530 -p 19531:19531 -p 8080:8080 milvusdb/milvus:v2.0.0-rc7-cpu

# elasticsearch
docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" docker.elastic.co/elasticsearch/elasticsearch:7.9.3