package com.boonya.game.controller;

import com.boonya.game.model.User;
import com.boonya.game.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

// 使用WebFlux实现响应式编程
@RestController
public class ReactiveUserController {

    @Autowired
    private UserService userService;

    @GetMapping("/users/{id}")
    public Mono<User> getUserReactive(@PathVariable Long id) {
        return Mono.fromCallable(() -> userService.getUser(id))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
