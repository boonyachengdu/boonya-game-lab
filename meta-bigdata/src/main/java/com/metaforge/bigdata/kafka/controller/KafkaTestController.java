package com.metaforge.bigdata.kafka.controller;

import com.metaforge.bigdata.kafka.producer.KafkaProducerService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kafka")
public class KafkaTestController {

    private final KafkaProducerService kafkaProducerService;

    public KafkaTestController(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    /**
     * 发送测试消息到Kafka
     */
    @PostMapping("/send-test")
    public String sendTestMessage(@RequestBody String message) {
        kafkaProducerService.sendMessage("database-cdc", message);
        return "Message sent: " + message;
    }
}
