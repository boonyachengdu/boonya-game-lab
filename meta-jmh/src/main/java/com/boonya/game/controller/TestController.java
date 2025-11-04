package com.boonya.game.controller;


import com.boonya.game.model.User;
import com.boonya.game.service.OrderService;
import com.boonya.game.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api")
public class TestController {

    @Autowired
    private UserService userService;

    @Autowired
    private OrderService orderService;

    @PostMapping("/users")
    public User createUser(@RequestParam String name,
                           @RequestParam String email) {
        User user = new User(null, name, email);
        return userService.save(user);
    }

    // 使用 Reactor webflux 异步非阻塞接口 方式获取用户信息
    @GetMapping("/users/{id}")
    public Mono<User> getUser(@PathVariable Long id) {
        return Mono.fromCallable(() -> userService.getUser(id))
                .subscribeOn(Schedulers.boundedElastic());
    }

    @PostMapping("/orders")
    public OrderService.Order createOrder(@RequestParam Long userId,
                                          @RequestParam Double amount) {
        return orderService.createOrder(userId, amount);
    }

    @PostMapping("/orders/{id}/complete")
    public Boolean completeOrder(@PathVariable Long id) {
        return orderService.completeOrder(id);
    }
}
