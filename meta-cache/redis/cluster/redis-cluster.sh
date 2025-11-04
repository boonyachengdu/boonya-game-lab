# -------------------------三主三从方案（高并发场景）

# 创建网络
docker network create redis-cluster-net

# 启动6个节点
for port in 7001 7002 7003 7004 7005 7006
do
  docker run -d --name redis-${port} \
    --net redis-cluster-net \
    -p ${port}:6379 -p 1${port}:16379 \
    -v $(pwd)/${port}/data:/data \
    -v $(pwd)/${port}/conf/redis.conf:/usr/local/etc/redis/redis.conf \
    redis:7-alpine redis-server /usr/local/etc/redis/redis.conf
done

# 创建集群
docker exec -it redis-7001 redis-cli --cluster create \
  172.18.0.2:6379 172.18.0.3:6379 172.18.0.4:6379 \
  172.18.0.5:6379 172.18.0.6:6379 172.18.0.7:6379 \
  --cluster-replicas 1 -a yourpassword