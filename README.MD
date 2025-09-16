# publish steps:

1. build image

`docker build -t docker.io/boonyadocker/boonya-game-lab:latest .`

2. log in docker hub

`docker login docker.io`

3. push image to docker hub.
-- push yourself docker hub
`docker push docker.io/boonyadocker/boonya-game-lab:latest`

# publish by k8s
-- as cluster
`kubectl apply -f deployment.yaml`

-- Pod status
`kubectl get pods`

-- deployment status
`kubectl get deployment boonya-game-lab`

-- service status
`kubectl get svc your-springboot-app-service`

-- delete cluster
`kubectl delete -f deployment.yaml`

# k8s dashboard  kuboard

**database support Mysql/MariaDB /OpenGauss**

https://github.com/eip-work/kuboard-press

https://kuboard.cn/v4/install/#%E8%87%B4-kuboard-v3-%E7%94%A8%E6%88%B7

MYSql/MariaDB:

`CREATE DATABASE kuboard DEFAULT CHARACTER SET = 'utf8mb4' DEFAULT COLLATE = 'utf8mb4_unicode_ci';
create user 'kuboard'@'%' identified by 'Kuboard123';
grant all privileges on kuboard.* to 'kuboard'@'%';
FLUSH PRIVILEGES;`

docker run command:
* cent-os
`sudo docker run -d \
--restart=unless-stopped \
--name=kuboard \
-p 80:80/tcp \
-e TZ="Asia/Shanghai" \
-e DB_DRIVER=com.mysql.cj.jdbc.Driver \
-e DB_URL="jdbc:mysql://10.99.0.8:3306/kuboard?serverTimezone=Asia/Shanghai" \
-e DB_USERNAME=kuboard \
-e DB_PASSWORD=Kuboard123 \
swr.cn-east-2.myhuaweicloud.com/kuboard/kuboard:v4`
* windows
`docker run -d --restart=unless-stopped --name=kuboard -p 80:80/tcp -e TZ="Asia/Shanghai" -e DB_DRIVER=com.mysql.cj.jdbc.Driver -e DB_URL="jdbc:mysql://127.0.0.1:3306/kuboard?serverTimezone=Asia/Shanghai" -e DB_USERNAME=kuboard -e DB_PASSWORD=Kuboard123 swr.cn-east-2.myhuaweicloud.com/kuboard/kuboard:v4`

**visit**

http://localhost
account/password: admin/Kuboard