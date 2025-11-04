package com.metaforge.bigdata.kafka.consumer;

// 添加监控指标
import com.metaforge.bigdata.spark.service.DataSyncS3aService;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class KafkaConsumerService {

    private final DataSyncS3aService sparkDataSyncService;

    private final MeterRegistry meterRegistry;// 监控指标

    public KafkaConsumerService(DataSyncS3aService sparkDataSyncService, MeterRegistry meterRegistry) {
        this.sparkDataSyncService = sparkDataSyncService;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(topics = "database-cdc", groupId = "spark-data-sync-group")
    public void consumeCdcData(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            long startTime = System.currentTimeMillis();
            sparkDataSyncService.processRealTimeData(record);
            ack.acknowledge();

            // 记录处理时间
            long processingTime = System.currentTimeMillis() - startTime;
            meterRegistry.timer("kafka.processing.time").record(processingTime, TimeUnit.MILLISECONDS);
            meterRegistry.counter("kafka.messages.processed").increment();

        } catch (Exception e) {
            meterRegistry.counter("kafka.messages.failed").increment();
            // 错误处理逻辑...
        }
    }
}