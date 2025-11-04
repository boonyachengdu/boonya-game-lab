#!/bin/bash

# 创建目录结构
for port in 7001 7002 7003 7004 7005 7006; do
  mkdir -p ./${port}/conf
  mkdir -p ./${port}/data

  cat > ./${port}/conf/redis.conf << EOF
port 6379
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
appendonly yes
requirepass yourpassword
masterauth yourpassword
dir /data
bind 0.0.0.0
EOF
done

# 启动集群
docker-compose -f docker-compose-cluster.yml up -d

# 等待节点启动
sleep 10

# 创建集群
docker exec -it redis-node1 redis-cli -a yourpassword --cluster create \
  172.18.0.2:6379 172.18.0.3:6379 172.18.0.4:6379 \
  172.18.0.5:6379 172.18.0.6:6379 172.18.0.7:6379 \
  --cluster-replicas 1