package com.boonya.game.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class OrderService {
    private final ConcurrentHashMap<Long, Order> orderCache = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public static class Order {
        private Long id;
        private Long userId;
        private Double amount;
        private String status;

        public Order(Long id, Long userId, Double amount, String status) {
            this.id = id;
            this.userId = userId;
            this.amount = amount;
            this.status = status;
        }

        // getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public Double getAmount() { return amount; }
        public void setAmount(Double amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public Order createOrder(Long userId, Double amount) {
        Long id = idGenerator.getAndIncrement();
        Order order = new Order(id, userId, amount, "CREATED");
        orderCache.put(id, order);

        // 模拟业务处理
        processOrder(order);
        return order;
    }

    private void processOrder(Order order) {
        // 模拟订单处理逻辑
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        order.setStatus("PROCESSED");
    }

    public Order getOrder(Long id) {
        return orderCache.get(id);
    }

    public boolean completeOrder(Long id) {
        Order order = orderCache.get(id);
        if (order != null) {
            order.setStatus("COMPLETED");
            return true;
        }
        return false;
    }
}
