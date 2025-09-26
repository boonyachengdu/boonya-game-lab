package com.boonya.game.jmh;

import com.boonya.game.model.User;
import com.boonya.game.BoonyaGameJmhApplication;
import com.boonya.game.service.OrderService;
import com.boonya.game.service.UserService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.AverageTime}) // 同时测量吞吐量和平均时间
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 3, time = 3)
@Threads(Threads.MAX) // 使用最大可用线程数
@Fork(1)
@State(Scope.Benchmark)
public class ServiceBenchmark {

    private ConfigurableApplicationContext context;
    private UserService userService;
    private OrderService orderService;

    @Setup
    public void setup() {
        context = SpringApplication.run(BoonyaGameJmhApplication.class);
        userService = context.getBean(UserService.class);
        orderService = context.getBean(OrderService.class);
    }

    @TearDown
    public void tearDown() {
        context.close();
    }

    @Benchmark
    public void testUserServiceThroughput(Blackhole blackhole) {
        User user = new User(1L, "bench_user_" + System.currentTimeMillis(),
                "bench@" + System.currentTimeMillis() + ".com");
        userService.save(user);
        blackhole.consume(user);
    }

    @Benchmark
    public void testOrderServiceThroughput(Blackhole blackhole) {
        OrderService.Order order = orderService.createOrder(1L, 99.9);
        blackhole.consume(order);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime) // 采样时间模式
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void testUserQueryLatency(Blackhole blackhole) {
        User user = userService.getUser(1L);
        blackhole.consume(user);
    }
}
