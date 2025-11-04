# 验证单点模式
docker exec -it redis-standalone redis-cli -a yourpassword info replication

# 验证集群模式
docker exec -it redis-node1 redis-cli -a yourpassword --cluster check 172.18.0.2:6379

# 验证哨兵模式
docker exec -it sentinel1 redis-cli -p 26379 info sentinel