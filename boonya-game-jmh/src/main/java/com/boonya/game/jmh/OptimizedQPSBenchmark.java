package com.boonya.game.jmh;

import com.boonya.game.component.ApplicationContextHolder;
import com.boonya.game.model.User;
import com.boonya.game.service.UserService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@State(Scope.Benchmark)
public class OptimizedQPSBenchmark {

    private UserService userService;
    private List<Long> testUserIds;

    @Setup
    public void setup() {
        //userService = createUserService();
        userService = ApplicationContextHolder.getBean(UserService.class);
        testUserIds = LongStream.range(1, 10000).boxed().collect(Collectors.toList());

        // 预热缓存
        testUserIds.parallelStream().forEach(userService::getUserOptimized);
    }

    @Benchmark
    @Threads(32) // 增加线程数
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testOptimizedQPS(Blackhole blackhole) {
        Long userId = testUserIds.get(ThreadLocalRandom.current().nextInt(testUserIds.size()));
        User user = userService.getUserOptimized(userId);
        blackhole.consume(user);
    }
}
