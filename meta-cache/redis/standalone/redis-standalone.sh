# 基本部署
docker run -d --name redis-standalone \
  -p 6379:6379 \
  -v /path/to/redis/data:/data \
  redis:7-alpine redis-server --appendonly yes

# 带密码和配置文件的部署
docker run -d --name redis-standalone \
  -p 6379:6379 \
  -v /path/to/redis/data:/data \
  -v /path/to/redis.conf:/usr/local/etc/redis/redis.conf \
  redis:7-alpine redis-server /usr/local/etc/redis/redis.conf --requirepass "yourpassword"