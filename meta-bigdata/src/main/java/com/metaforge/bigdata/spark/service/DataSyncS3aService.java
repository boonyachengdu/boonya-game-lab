package com.metaforge.bigdata.spark.service;

import com.metaforge.bigdata.spark.model.ChangeData;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.spark.sql.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 通过Spark将Mysql数据同步到数据湖仓
 */
@Service
public class DataSyncS3aService {

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

    private static final String OP_CREATE = "c";
    private static final String OP_UPDATE = "u";
    private static final String OP_DELETE = "d";
    private static final String OP_UNKNOWN = "x";

    private static final String TYPE_INSERT = "INSERT";
    private static final String TYPE_UPDATE = "UPDATE";
    private static final String TYPE_DELETE = "DELETE";

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
        List<String> failedTables = new ArrayList<>();
        for (String tableName : tableNames) {
            try {
                syncMySQLToDataLake(tableName);
                System.out.println("Successfully synced table: " + tableName);
            } catch (Exception e) {
                System.err.println("Failed to sync table: " + tableName + ", Error: " + e.getMessage());
                failedTables.add(tableName);
            }
        }
        if (!failedTables.isEmpty()) {
            System.err.println("The following tables failed to sync: " + failedTables);
        }
    }

    /**
     * 实时处理Debezium/Canal数据
     */
    public void processRealTimeData(ConsumerRecord<String, String> record) {
        ChangeData changeData = parseChangeData(record.value());

        switch (changeData.getOp()) {
            case OP_CREATE:
                handleInsert(changeData);
                break;
            case OP_UPDATE:
                handleUpdate(changeData);
                break;
            case OP_DELETE:
                handleDelete(changeData);
                break;
            default:
                System.out.println("Unknown operation type: " + changeData.getOp());
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
                parseDebeziumFormat(rootNode, changeData);
            } else if (rootNode.has("isDdl") && !rootNode.get("isDdl").asBoolean()) {
                parseCanalFormat(rootNode, changeData);
            }

            return changeData;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse change data: " + jsonData, e);
        }
    }

    private void parseDebeziumFormat(JsonNode rootNode, ChangeData changeData) {
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
    }

    private void parseCanalFormat(JsonNode rootNode, ChangeData changeData) {
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

        if (TYPE_INSERT.equals(type)) {
            changeData.setAfter(data.get(0));
        } else if (TYPE_UPDATE.equals(type)) {
            changeData.setBefore(old.get(0));
            changeData.setAfter(data.get(0));
        } else if (TYPE_DELETE.equals(type)) {
            changeData.setBefore(data.get(0));
        }

        if (rootNode.has("table")) {
            changeData.setTable(rootNode.get("table").asText());
        }
        if (rootNode.has("database")) {
            changeData.setDatabase(rootNode.get("database").asText());
        }
    }

    private String convertCanalTypeToOp(String canalType) {
        switch (canalType) {
            case TYPE_INSERT: return OP_CREATE;
            case TYPE_UPDATE: return OP_UPDATE;
            case TYPE_DELETE: return OP_DELETE;
            default: return OP_UNKNOWN;
        }
    }

    private Map<String, Object> convertJsonNodeToMap(JsonNode node) {
        if (node == null) return new HashMap<>();

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

    private void handleInsert(ChangeData changeData) {
        try {
            System.out.println("Processing INSERT operation for table: " + changeData.getTable());
            writeToDataLake(changeData.getTable(), changeData.getAfter(), TYPE_INSERT);
        } catch (Exception e) {
            System.err.println("Failed to process INSERT operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleUpdate(ChangeData changeData) {
        try {
            System.out.println("Processing UPDATE operation for table: " + changeData.getTable());
            writeToDataLake(changeData.getTable(), changeData.getAfter(), TYPE_UPDATE);
        } catch (Exception e) {
            System.err.println("Failed to process UPDATE operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleDelete(ChangeData changeData) {
        try {
            System.out.println("Processing DELETE operation for table: " + changeData.getTable());
            markAsDeletedInDataLake(changeData.getTable(), changeData.getBefore());
        } catch (Exception e) {
            System.err.println("Failed to process DELETE operation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void writeToDataLake(String table, Map<String, Object> data, String operation) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            List<String> jsonList = Collections.singletonList(jsonData);
            Dataset<Row> df = sparkSession.read().json(sparkSession.createDataset(jsonList, Encoders.STRING()));
            df = df.withColumn("__operation_type", org.apache.spark.sql.functions.lit(operation))
                    .withColumn("__processing_time", org.apache.spark.sql.functions.current_timestamp());
            String outputPath = dataLakePath + table;
            df.write()
                    .format("parquet")
                    .mode(SaveMode.Append)
                    .save(outputPath);
            System.out.println("Successfully wrote to data lake: " + outputPath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write to data lake", e);
        }
    }

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

    private org.apache.spark.sql.types.StructType getSchemaFromMap(Map<String, Object> data) {
        List<org.apache.spark.sql.types.StructField> fields = new ArrayList<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String fieldName = entry.getKey();
            Object value = entry.getValue();

            org.apache.spark.sql.types.DataType dataType;
            if (value instanceof String) {
                dataType = org.apache.spark.sql.types.DataTypes.StringType;
            } else if (value instanceof Integer) {
                dataType = org.apache.spark.sql.types.DataTypes.IntegerType;
            } else if (value instanceof Long) {
                dataType = org.apache.spark.sql.types.DataTypes.LongType;
            } else if (value instanceof Double) {
                dataType = org.apache.spark.sql.types.DataTypes.DoubleType;
            } else if (value instanceof Boolean) {
                dataType = org.apache.spark.sql.types.DataTypes.BooleanType;
            } else {
                dataType = org.apache.spark.sql.types.DataTypes.StringType;
            }

            fields.add(org.apache.spark.sql.types.DataTypes.createStructField(
                    fieldName, dataType, true));
        }

        fields.add(org.apache.spark.sql.types.DataTypes.createStructField(
                "__operation_type", org.apache.spark.sql.types.DataTypes.StringType, true));
        fields.add(org.apache.spark.sql.types.DataTypes.createStructField(
                "__processing_time", org.apache.spark.sql.types.DataTypes.TimestampType, true));

        return org.apache.spark.sql.types.DataTypes.createStructType(fields);
    }
}
