package com.boonya.game.datasource;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 动态数据源持有者，用于在运行时切换数据源
 * 基于ThreadLocal实现线程安全的数据源切换
 */
public class DynamicDataSourceHolder {

    /**
     * 数据源常量定义
     */
    public static final String MASTER = "master";
    public static final String SLAVE = "slave";
    public static final String SLAVE_1 = "slave1";
    public static final String SLAVE_2 = "slave2";

    /**
     * 使用ThreadLocal保证线程安全
     */
    private static final ThreadLocal<String> DATASOURCE_HOLDER = new ThreadLocal<>();

    /**
     * 从库计数器，用于负载均衡
     */
    private static final AtomicInteger SLAVE_COUNTER = new AtomicInteger(0);

    /**
     * 可用的从库列表
     */
    private static final List<String> SLAVE_DATA_SOURCES = new ArrayList<>();

    static {
        // 初始化从库列表
        SLAVE_DATA_SOURCES.add(SLAVE_1);
        SLAVE_DATA_SOURCES.add(SLAVE_2);
    }

    /**
     * 设置数据源
     * @param dataSource 数据源名称
     */
    public static void setDataSource(String dataSource) {
        if (StringUtils.hasText(dataSource)) {
            DATASOURCE_HOLDER.set(dataSource);
        }
    }

    /**
     * 获取当前数据源
     * @return 数据源名称
     */
    public static String getDataSource() {
        String dataSource = DATASOURCE_HOLDER.get();

        // 如果未指定数据源，默认使用主库
        if (!StringUtils.hasText(dataSource)) {
            return MASTER;
        }

        // 如果指定了slave，则进行负载均衡
        if (SLAVE.equals(dataSource)) {
            return getLoadBalancedSlave();
        }

        return dataSource;
    }

    /**
     * 清除数据源设置
     */
    public static void clearDataSource() {
        DATASOURCE_HOLDER.remove();
    }

    /**
     * 强制使用主库
     */
    public static void useMaster() {
        setDataSource(MASTER);
    }

    /**
     * 使用从库（负载均衡）
     */
    public static void useSlave() {
        setDataSource(SLAVE);
    }

    /**
     * 使用特定的从库
     * @param slaveName 从库名称
     */
    public static void useSpecificSlave(String slaveName) {
        if (SLAVE_DATA_SOURCES.contains(slaveName)) {
            setDataSource(slaveName);
        } else {
            throw new IllegalArgumentException("Unknown slave data source: " + slaveName);
        }
    }

    /**
     * 获取负载均衡后的从库
     * @return 从库名称
     */
    private static String getLoadBalancedSlave() {
        if (SLAVE_DATA_SOURCES.isEmpty()) {
            throw new IllegalStateException("No slave data sources available");
        }

        int index = Math.abs(SLAVE_COUNTER.getAndIncrement() % SLAVE_DATA_SOURCES.size());
        return SLAVE_DATA_SOURCES.get(index);
    }

    /**
     * 添加从库数据源
     * @param slaveDataSource 从库名称
     */
    public static void addSlaveDataSource(String slaveDataSource) {
        if (StringUtils.hasText(slaveDataSource) && !SLAVE_DATA_SOURCES.contains(slaveDataSource)) {
            SLAVE_DATA_SOURCES.add(slaveDataSource);
        }
    }

    /**
     * 移除从库数据源
     * @param slaveDataSource 从库名称
     */
    public static void removeSlaveDataSource(String slaveDataSource) {
        SLAVE_DATA_SOURCES.remove(slaveDataSource);
    }

    /**
     * 获取所有可用的从库数据源
     * @return 从库列表
     */
    public static List<String> getAvailableSlaveDataSources() {
        return new ArrayList<>(SLAVE_DATA_SOURCES);
    }

    /**
     * 获取当前线程是否使用了主库
     * @return 是否使用主库
     */
    public static boolean isUsingMaster() {
        String currentDataSource = getDataSource();
        return MASTER.equals(currentDataSource);
    }

    /**
     * 获取当前线程是否使用了从库
     * @return 是否使用从库
     */
    public static boolean isUsingSlave() {
        return !isUsingMaster();
    }

    /**
     * 获取当前数据源信息（用于日志和调试）
     * @return 数据源信息
     */
    public static String getDataSourceInfo() {
        String dataSource = getDataSource();
        return String.format("DataSource[%s], Thread[%s]",
                dataSource, Thread.currentThread().getName());
    }

    /**
     * 重置从库计数器（主要用于测试）
     */
    public static void resetSlaveCounter() {
        SLAVE_COUNTER.set(0);
    }

    /**
     * 获取从库数量
     * @return 从库数量
     */
    public static int getSlaveCount() {
        return SLAVE_DATA_SOURCES.size();
    }
}
