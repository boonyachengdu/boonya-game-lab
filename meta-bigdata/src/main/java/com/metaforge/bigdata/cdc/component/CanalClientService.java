package com.metaforge.bigdata.cdc.component;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.Message;
import com.alibaba.otter.canal.protocol.CanalEntry.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CDC通过Canal监听回传数据
 */
@Component
public class CanalClientService {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public void startCanalClientByDemon(CanalConnector connector) {
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    startCanalClient(connector);
                } catch (Exception e) {
                    // 异常处理（如记录日志），然后等待后重试
                    System.err.println("Canal客户端异常，5秒后重试: " + e.getMessage());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            connector.disconnect(); // 确保最终断开连接
        });
    }

    public void stop() {
        executor.shutdownNow(); // 终止线程
    }

    private void startCanalClient(CanalConnector connector) throws Exception {
        connector.connect();
        connector.subscribe(".*\\..*");
        connector.rollback();

        while (true) {
            Message message = connector.getWithoutAck(100);
            long batchId = message.getId();
            int size = message.getEntries().size();

            if (batchId == -1 || size == 0) {
                Thread.sleep(1000);
                continue;
            }

            processEntry(message.getEntries());
            connector.ack(batchId);
        }
    }

    private void processEntry(List<Entry> entries) {
        for (Entry entry : entries) {
            if (entry.getEntryType() == EntryType.TRANSACTIONBEGIN ||
                    entry.getEntryType() == EntryType.TRANSACTIONEND) {
                continue;
            }

            RowChange rowChange;
            try {
                rowChange = RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                throw new RuntimeException("解析RowChange错误", e);
            }

            EventType eventType = rowChange.getEventType();

            for (RowData rowData : rowChange.getRowDatasList()) {
                switch (eventType) {
                    case INSERT:
                        handleInsert(rowData.getAfterColumnsList());
                        break;
                    case UPDATE:
                        handleUpdate(rowData.getBeforeColumnsList(), rowData.getAfterColumnsList());
                        break;
                    case DELETE:
                        handleDelete(rowData.getBeforeColumnsList());
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void handleInsert(List<Column> columns) {
        // 处理插入操作
        System.out.println("INSERT操作:");
        for (Column column : columns) {
            System.out.println(column.getName() + " : " + column.getValue() +
                    "    update=" + column.getUpdated());
        }
    }

    private void handleUpdate(List<Column> beforeColumns, List<Column> afterColumns) {
        // 处理更新操作
        System.out.println("UPDATE操作:");
        System.out.println("更新前:");
        for (Column column : beforeColumns) {
            if (column.getUpdated()) {
                System.out.println(column.getName() + " : " + column.getValue());
            }
        }
        System.out.println("更新后:");
        for (Column column : afterColumns) {
            if (column.getUpdated()) {
                System.out.println(column.getName() + " : " + column.getValue());
            }
        }
    }

    private void handleDelete(List<Column> columns) {
        // 处理删除操作
        System.out.println("DELETE操作:");
        for (Column column : columns) {
            System.out.println(column.getName() + " : " + column.getValue());
        }
    }
}
