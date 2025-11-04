package com.metaforge.bigdata.spark.service;

import com.metaforge.bigdata.spark.model.ChangeData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.spark.sql.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 通过Spark将kafka消息数据同步到数据湖仓MinIo
 */
@Service
public class DataSyncMinIoService {

    @Autowired
    private SparkSession sparkSession;

    @Value("${mysql.url}")
    private String mysqlUrl;

    @Value("${mysql.user}")
    private String mysqlUser;

    @Value("${mysql.password}")
    private String mysqlPassword;

    @Value("${data-lake.path}")
    private String dataLakePath;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 将MySQL数据同步到大数据平台
     */
    public void syncMySQLToDataLake(String tableName) {
        Dataset<Row> jdbcDF = sparkSession.read()
                .format("jdbc")
                .option("url", mysqlUrl)
                .option("dbtable", tableName)
                .option("user", mysqlUser)
                .option("password", mysqlPassword)
                .load();

        String outputPath = dataLakePath + tableName;
        jdbcDF.write()
                .format("parquet")
                .mode(SaveMode.Append)
                .save(outputPath);
    }

    /**
     * 批量同步多个MySQL表到数据湖
     */
    public void batchSyncTablesToDataLake(List<String> tableNames) {
        for (String tableName : tableNames) {
            try {
                syncMySQLToDataLake(tableName);
                System.out.println("Successfully synced table: " + tableName);
            } catch (Exception e) {
                System.err.println("Failed to sync table: " + tableName + ", Error: " + e.getMessage());
                // 可扩展为记录日志、加入失败队列、重试等
            }
        }
    }

    /**
     * 实时处理Debezium/Canal数据
     */
    public void processRealTimeData(ConsumerRecord<String, String> record) {
        try {
            ChangeData changeData = parseChangeData(record.value());

            System.out.println("Processing Kafka message - Topic: " + record.topic() +
                    ", Partition: " + record.partition() +
                    ", Offset: " + record.offset());

            switch (changeData.getOp()) {
                case "c": // create
                    handleInsert(changeData);
                    break;
                case "u": // update
                    handleUpdate(changeData);
                    break;
                case "d": // delete
                    handleDelete(changeData);
                    break;
                default:
                    System.out.println("Unknown operation type: " + changeData.getOp());
            }
        } catch (Exception e) {
            System.err.println("Failed to process real-time data: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process real-time data", e);
        }
    }

    /**
     * 解析变更数据
     */
    private ChangeData parseChangeData(String jsonData) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonData);
            ChangeData changeData = new ChangeData();

            if (rootNode.has("op")) {
                changeData.setOp(rootNode.get("op").asText());

                if (rootNode.has("before")) {
                    changeData.setBefore(convertJsonNodeToMap(rootNode.get("before")));
                }
                if (rootNode.has("after")) {
                    changeData.setAfter(convertJsonNodeToMap(rootNode.get("after")));
                }

                if (rootNode.has("source")) {
                    JsonNode source = rootNode.get("source");
                    if (source.has("table")) {
                        changeData.setTable(source.get("table").asText());
                    }
                    if (source.has("db")) {
                        changeData.setDatabase(source.get("db").asText());
                    }
                }
            } else if (rootNode.has("isDdl") && !rootNode.get("isDdl").asBoolean()) {
                String type = rootNode.get("type").asText();
                changeData.setOp(convertCanalTypeToOp(type));

                List<Map<String, Object>> data = new ArrayList<>();
                JsonNode dataNode = rootNode.get("data");
                if (dataNode.isArray()) {
                    for (JsonNode item : dataNode) {
                        data.add(convertJsonNodeToMap(item));
                    }
                }

                List<Map<String, Object>> old = new ArrayList<>();
                JsonNode oldNode = rootNode.get("old");
                if (oldNode != null && oldNode.isArray()) {
                    for (JsonNode item : oldNode) {
                        old.add(convertJsonNodeToMap(item));
                    }
                }

                if ("INSERT".equals(type)) {
                    changeData.setAfter(data.get(0));
                } else if ("UPDATE".equals(type)) {
                    changeData.setBefore(old.get(0));
                    changeData.setAfter(data.get(0));
                } else if ("DELETE".equals(type)) {
                    changeData.setBefore(data.get(0));
                }

                if (rootNode.has("table")) {
                    changeData.setTable(rootNode.get("table").asText());
                }
                if (rootNode.has("database")) {
                    changeData.setDatabase(rootNode.get("database").asText());
                }
            }

            return changeData;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse change data: " + jsonData, e);
        }
    }

    /**
     * 将Canal类型转换为操作代码
     */
    private String convertCanalTypeToOp(String canalType) {
        switch (canalType) {
            case "INSERT":
                return "c";
            case "UPDATE":
                return "u";
            case "DELETE":
                return "d";
            default:
                return "x";
        }
    }

    /**
     * 将JsonNode转换为Map
     */
    private Map<String, Object> convertJsonNodeToMap(JsonNode node) {
        Map<String, Object> result = new HashMap<>();
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isTextual()) {
                result.put(entry.getKey(), value.asText());
            } else if (value.isNumber()) {
                result.put(entry.getKey(), value.numberValue());
            } else if (value.isBoolean()) {
                result.put(entry.getKey(), value.asBoolean());
            } else if (value.isNull()) {
                result.put(entry.getKey(), null);
            } else {
                result.put(entry.getKey(), value.toString());
            }
        });
        return result;
    }

    /**
     * 处理插入操作
     */
    private void handleInsert(ChangeData changeData) {
        try {
            System.out.println("Processing INSERT operation for table: " + changeData.getTable());
            writeToDataLake(changeData.getTable(), changeData.getAfter(), "INSERT");
        } catch (Exception e) {
            System.err.println("Failed to process INSERT operation: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process INSERT operation", e);
        }
    }

    /**
     * 处理更新操作
     */
    private void handleUpdate(ChangeData changeData) {
        try {
            System.out.println("Processing UPDATE operation for table: " + changeData.getTable());
            writeToDataLake(changeData.getTable(), changeData.getAfter(), "UPDATE");
        } catch (Exception e) {
            System.err.println("Failed to process UPDATE operation: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process UPDATE operation", e);
        }
    }

    /**
     * 处理删除操作
     */
    private void handleDelete(ChangeData changeData) {
        try {
            System.out.println("Processing DELETE operation for table: " + changeData.getTable());
            markAsDeletedInDataLake(changeData.getTable(), changeData.getBefore());
        } catch (Exception e) {
            System.err.println("Failed to process DELETE operation: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to process DELETE operation", e);
        }
    }

    /**
     * 写入数据湖
     */
    private void writeToDataLake(String table, Map<String, Object> data, String operation) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            List<String> jsonList = Collections.singletonList(jsonData);
            Dataset<Row> df = sparkSession.read().json(sparkSession.createDataset(jsonList, Encoders.STRING()));

            df = df.withColumn("__operation_type", org.apache.spark.sql.functions.lit(operation))
                    .withColumn("__processing_time", org.apache.spark.sql.functions.current_timestamp())
                    .withColumn("__kafka_timestamp", org.apache.spark.sql.functions.current_timestamp());

            String outputPath = dataLakePath + "raw/" + table;
            df.write()
                    .format("parquet")
                    .mode(SaveMode.Append)
                    .save(outputPath);

            System.out.println("Successfully wrote to data lake: " + outputPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to data lake", e);
        }
    }

    /**
     * 在数据湖中标记删除
     */
    private void markAsDeletedInDataLake(String table, Map<String, Object> data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            List<String> jsonList = Collections.singletonList(jsonData);
            Dataset<Row> df = sparkSession.read().json(sparkSession.createDataset(jsonList, Encoders.STRING()));

            df = df.withColumn("__deleted", org.apache.spark.sql.functions.lit(true))
                    .withColumn("__deletion_time", org.apache.spark.sql.functions.current_timestamp());

            String outputPath = dataLakePath + table + "_deletes";
            df.write()
                    .format("parquet")
                    .mode(SaveMode.Append)
                    .save(outputPath);

            System.out.println("Successfully marked as deleted in data lake: " + outputPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to mark as deleted in data lake", e);
        }
    }
}
