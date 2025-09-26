# Getting Started

JMH Benchmark tests.

# Commands for JMH

## 编译项目 

`mvn clean compile`

## 运行QPS测试

`mvn exec:java -Dexec.mainClass=org.openjdk.jmh.Main -Dexec.args=com.boonya.game.jmh.QPSBenchmark`

## 运行TPS测试

`mvn exec:java -Dexec.mainClass=org.openjdk.jmh.Main -Dexec.args=com.boonya.game.jmh.TPSBenchmark`

## 运行所有基准测试

`mvn exec:java -Dexec.mainClass=org.openjdk.jmh.Main -Dexec.args=.*Benchmark`

## 生成可执行的JAR并运行

`mvn clean package`

`java -jar target/benchmarks.jar com.boonya.game.jmh.QPSBenchmark`

## Profile 运行

`mvn clean compile exec:java -Dexec.mainClass="org.openjdk.jmh.Main" -Dexec.args="com.boonya.game.jmh.QPSBenchmark -f 1 -wi 3 -i 5"`

# Idea Plugin

Idea JML plugin can test like Junit.

# 优化

## 目标

经过上述优化，QPS提升路径：

* 优化阶段	预期QPS	主要优化手段
* 基础版本	2,000	基础实现
* 代码优化	5,000	对象池、无锁数据结构
* 缓存优化	15,000	多级缓存、缓存预热
* 数据库优化	20,000	连接池、读写分离
* 架构优化	30,000+	异步、批量处理

## 优化手段

### 代码层面

* 减少对象的创建和GC压力
* 使用无锁数据结构

### 缓存优化

* 多级缓存架构
* 缓存预热和预加载

### 数据库优化

* 连接池参数优化
* 读写分离、分库分表

### JVM和系统优化

* 优化JVM参数
* 操作系统优化

### 架构层面

* 异步和非阻塞处理
* 批量处理优化

### 监控和调优工具

* JMH优化测试
* 性能监控配置


