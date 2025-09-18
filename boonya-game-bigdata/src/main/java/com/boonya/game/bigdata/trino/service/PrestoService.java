package com.boonya.game.bigdata.trino.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
public class PrestoService {

    @Value("${presto.url:jdbc:trino://localhost:8080}")
    private String prestoUrl;

    @Value("${presto.user:admin}")
    private String prestoUser;

    @Value("${presto.catalog:hive}")
    private String prestoCatalog;

    @Value("${presto.schema:default}")
    private String prestoSchema;

    @Value("${presto.source:spring-boot}")
    private String prestoSource;

    /**
     * 获取Presto数据库连接
     */
    private Connection getConnection() throws SQLException {
        try {
            // 加载Trino JDBC驱动 (Presto重命名为Trino)
            Class.forName("io.trino.jdbc.TrinoDriver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("Trino JDBC driver not found", e);
        }

        // 配置连接属性
        Properties properties = new Properties();
        properties.setProperty("user", prestoUser);
        properties.setProperty("catalog", prestoCatalog);
        properties.setProperty("schema", prestoSchema);
        properties.setProperty("source", prestoSource);
        properties.setProperty("SSL", "false"); // 根据实际情况调整

        return DriverManager.getConnection(prestoUrl, properties);
    }

    /**
     * 执行查询并返回结果列表
     */
    public List<Map<String, Object>> executeQuery(String sql, int limit) throws SQLException {
        // 如果SQL不包含LIMIT，则添加LIMIT子句
        if (!sql.toLowerCase().contains("limit") && limit > 0) {
            sql += " LIMIT " + limit;
        }

        List<Map<String, Object>> resultList = new ArrayList<>();

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 获取列名
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }

            // 处理结果集
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = columnNames.get(i - 1);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                resultList.add(row);
            }
        }

        return resultList;
    }

    /**
     * 执行查询并返回单个值
     */
    public Object executeScalar(String sql) throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (resultSet.next()) {
                return resultSet.getObject(1);
            }
            return null;
        }
    }

    /**
     * 执行更新操作（INSERT/UPDATE/DELETE）
     */
    public int executeUpdate(String sql) throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            return statement.executeUpdate(sql);
        }
    }

    /**
     * 获取数据库中的所有表
     */
    public List<String> listTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = "SHOW TABLES";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                tables.add(resultSet.getString(1));
            }
        }

        return tables;
    }

    /**
     * 获取指定表的所有列
     */
    public List<String> listColumns(String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        String sql = "DESCRIBE " + tableName;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                columns.add(resultSet.getString(1));
            }
        }

        return columns;
    }

    /**
     * 获取表的元数据信息
     */
    public Map<String, String> getTableMetadata(String tableName) throws SQLException {
        Map<String, String> metadata = new HashMap<>();
        String sql = "SHOW CREATE TABLE " + tableName;

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            if (resultSet.next()) {
                metadata.put("create_table_ddl", resultSet.getString(1));
            }
        }

        return metadata;
    }

    /**
     * 测试连接是否可用
     */
    public boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(5); // 5秒超时
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 获取Presto集群信息
     */
    public Map<String, Object> getClusterInfo() throws SQLException {
        Map<String, Object> info = new HashMap<>();

        // 获取节点信息
        String nodeSql = "SELECT node_id, http_uri, state FROM system.runtime.nodes";
        List<Map<String, Object>> nodes = executeQuery(nodeSql, 100);
        info.put("nodes", nodes);

        // 获取查询统计信息
        String querySql = "SELECT state, count(*) as count FROM system.runtime.queries GROUP BY state";
        List<Map<String, Object>> queryStats = executeQuery(querySql, 100);
        info.put("query_stats", queryStats);

        // 获取内存池信息
        String memorySql = "SELECT * FROM system.runtime.memory_pools";
        List<Map<String, Object>> memoryPools = executeQuery(memorySql, 100);
        info.put("memory_pools", memoryPools);

        return info;
    }

    /**
     * 批量执行查询（适用于大量数据处理）
     */
    public void executeBatch(List<String> queries) throws SQLException {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            for (String query : queries) {
                statement.addBatch(query);
            }

            statement.executeBatch();
        }
    }

    /**
     * 使用预编译语句执行查询（防止SQL注入）
     */
    public List<Map<String, Object>> executePreparedQuery(String sql, Object... params) throws SQLException {
        List<Map<String, Object>> resultList = new ArrayList<>();

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // 设置参数
            for (int i = 0; i < params.length; i++) {
                statement.setObject(i + 1, params[i]);
            }

            // 执行查询
            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();

                // 获取列名
                List<String> columnNames = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(metaData.getColumnName(i));
                }

                // 处理结果集
                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = columnNames.get(i - 1);
                        Object value = resultSet.getObject(i);
                        row.put(columnName, value);
                    }
                    resultList.add(row);
                }
            }
        }

        return resultList;
    }
}
