package com.boonya.game.jmh;

import com.boonya.game.model.User;
import com.boonya.game.BoonyaGameJmhApplication;
import com.boonya.game.service.UserService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@BenchmarkMode(Mode.Throughput) // 测量吞吐量 (QPS)
@OutputTimeUnit(TimeUnit.SECONDS) // 输出单位：秒
@Warmup(iterations = 3, time = 1) // 预热3轮，每轮1秒
@Measurement(iterations = 5, time = 2) // 测试5轮，每轮2秒
@Threads(4) // 使用4个线程
@Fork(1) //  fork 1个进程
@State(Scope.Benchmark) // 基准测试状态
public class QPSBenchmark {

    private ConfigurableApplicationContext context;
    private UserService userService;

    private AtomicLong userIdGenerator = new AtomicLong(1);

    @Setup
    public void setup() {
        // 启动Spring Boot应用
        context = SpringApplication.run(BoonyaGameJmhApplication.class);
        userService = context.getBean(UserService.class);

        // 预创建一些测试数据
        for (int i = 0; i < 1000; i++) {
            User user = new User(userIdGenerator.get(), "user" + i, "user" + i + "@example.com");
            userService.save(user);
        }
    }

    @TearDown
    public void tearDown() {
        context.close();
    }

    @Benchmark
    public void testUserQueryQPS(Blackhole blackhole) {
        // 模拟查询操作，测试QPS
        Long userId = (long) (System.nanoTime() % 1000 + 1);
        User user = userService.getUser(userId);
        blackhole.consume(user);
    }

    @Benchmark
    public void testUserCreateQPS() {
        // 模拟创建操作，测试QPS
        long timestamp = System.currentTimeMillis();
        User user = new User(userIdGenerator.get(), "benchmark_user_" + timestamp,
                "benchmark_" + timestamp + "@example.com");
        userService.save(user);
    }
}