# 选择一个轻量级的Java基础镜像
FROM openjdk:17.0.1-jdk-slim
# 使用 RUN 命令创建目录
RUN mkdir -p /app
# 将打包好的jar包复制到镜像中，并重命名（方便引用）
COPY target/boonya-game-lab-0.0.1-SNAPSHOT.jar /app/app.jar
# 切换工作目录/app
WORKDIR /app
# 声明容器运行时暴露的端口（与你的SpringBoot应用端口一致）
EXPOSE 8080
# 设置容器启动时执行的命令
ENTRYPOINT ["java", "-jar", "app.jar"]

# 先删除旧的容器（如果存在）
# docker rm -f boonya-game-lab
# 运行更新后的镜像
# docker run -d --name boonya-game-lab -p 8080:8080 docker.io/boonyadocker/boonya-game-lab:latest