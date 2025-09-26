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
import java.util.concurrent.atomic.AtomicLong;

@BenchmarkMode(Mode.Throughput) // 测量吞吐量 (TPS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Threads(8) // 使用8个线程模拟并发事务
@Fork(1)
@State(Scope.Benchmark)
public class TPSBenchmark {

    private ConfigurableApplicationContext context;
    private UserService userService;
    private OrderService orderService;
    private AtomicLong userIdGenerator;

    @Setup
    public void setup() {
        context = SpringApplication.run(BoonyaGameJmhApplication.class);
        userService = context.getBean(UserService.class);
        orderService = context.getBean(OrderService.class);
        userIdGenerator = new AtomicLong(1);

        // 预创建用户数据
        for (int i = 0; i < 100; i++) {
            User user = new User(userIdGenerator.getAndIncrement(), "pre_user_" + i, "pre_" + i + "@example.com");
            userService.save(user);
        }
    }

    @TearDown
    public void tearDown() {
        context.close();
    }

    @Benchmark
    public void testOrderTransactionTPS(Blackhole blackhole) {
        // 模拟完整的事务：创建用户 -> 创建订单 -> 完成订单
        Long userId = userIdGenerator.getAndIncrement();

        // 事务开始：创建用户
        User user = new User(userIdGenerator.getAndIncrement(), "transaction_user_" + userId,
                "transaction_" + userId + "@example.com");
        userService.save(user);

        // 创建订单
        OrderService.Order order = orderService.createOrder(user.getId(), 100.0);

        // 完成订单
        boolean success = orderService.completeOrder(order.getId());

        blackhole.consume(user);
        blackhole.consume(order);
        blackhole.consume(success);
    }

    @Benchmark
    public void testSimpleOrderTPS(Blackhole blackhole) {
        // 模拟简单的订单创建TPS
        Long userId = (long) (System.nanoTime() % 100 + 1);
        OrderService.Order order = orderService.createOrder(userId, 50.0);
        blackhole.consume(order);
    }
}
