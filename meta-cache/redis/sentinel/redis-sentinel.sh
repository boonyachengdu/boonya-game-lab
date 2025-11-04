#-----------------高可用方案-----------------
# 创建网络
docker network create redis-sentinel-net

# 启动主节点
docker run -d --name redis-master \
  --net redis-sentinel-net \
  -p 6379:6379 \
  -v $(pwd)/master/data:/data \
  -v $(pwd)/master/redis.conf:/usr/local/etc/redis/redis.conf \
  redis:7-alpine redis-server /usr/local/etc/redis/redis.conf

# 启动从节点
docker run -d --name redis-slave1 \
  --net redis-sentinel-net \
  -p 6380:6379 \
  -v $(pwd)/slave1/data:/data \
  -v $(pwd)/slave1/redis.conf:/usr/local/etc/redis/redis.conf \
  redis:7-alpine redis-server /usr/local/etc/redis/redis.conf --slaveof redis-master 6379

docker run -d --name redis-slave2 \
  --net redis-sentinel-net \
  -p 6381:6379 \
  -v $(pwd)/slave2/data:/data \
  -v $(pwd)/slave2/redis.conf:/usr/local/etc/redis/redis.conf \
  redis:7-alpine redis-server /usr/local/etc/redis/redis.conf --slaveof redis-master 6379

# 启动Sentinel节点
for port in 26379 26380 26381
do
  docker run -d --name sentinel-${port} \
    --net redis-sentinel-net \
    -p ${port}:26379 \
    -v $(pwd)/sentinel${port}/sentinel.conf:/usr/local/etc/redis/sentinel.conf \
    redis:7-alpine redis-sentinel /usr/local/etc/redis/sentinel.conf
done