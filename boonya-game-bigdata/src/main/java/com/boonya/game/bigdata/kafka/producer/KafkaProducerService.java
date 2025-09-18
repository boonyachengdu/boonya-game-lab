package com.boonya.game.bigdata.kafka.producer;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * 发送消息到Kafka主题（用于测试）
     */
    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
              /*  .addCallback(
                        result -> System.out.println("Message sent successfully: " + message),
                        ex -> System.err.println("Failed to send message: " + ex.getMessage())
                );*/
    }
}
