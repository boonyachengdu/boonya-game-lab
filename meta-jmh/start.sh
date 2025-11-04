# 启动参数
java -jar your-app.jar \
-Xmx4g -Xms4g \
-XX:+UseG1GC \
-XX:MaxGCPauseMillis=200 \
-XX:ParallelGCThreads=4 \
-XX:ConcGCThreads=2 \
-XX:+UnlockExperimentalVMOptions \
-XX:+UseContainerSupport \
-XX:MaxMetaspaceSize=512m \
-Djava.awt.headless=true \
-Djava.net.preferIPv4Stack=true