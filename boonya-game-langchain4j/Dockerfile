# Dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

# 复制构建产物
COPY target/boonya-game-langchain4j-*.jar app.jar

# 安装中文字体（解决PDF中文显示问题）
RUN apt-get update && apt-get install -y \
    fonts-wqy-microhei \
    fonts-wqy-zenhei \
    && rm -rf /var/lib/apt/lists/*

# 创建非root用户
RUN groupadd -r spring && useradd -r -g spring spring
USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]