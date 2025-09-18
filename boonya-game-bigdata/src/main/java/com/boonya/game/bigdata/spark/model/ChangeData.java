package com.boonya.game.bigdata.spark.model;

import java.util.Map;

/**
 * ChangeData 内部类定义
 */
public class ChangeData {
    private String op;
    private String table;
    private String database;
    private Map<String, Object> before;
    private Map<String, Object> after;

    // 构造函数、getter和setter方法
    public ChangeData() {
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public Map<String, Object> getBefore() {
        return before;
    }

    public void setBefore(Map<String, Object> before) {
        this.before = before;
    }

    public Map<String, Object> getAfter() {
        return after;
    }

    public void setAfter(Map<String, Object> after) {
        this.after = after;
    }

    @Override
    public String toString() {
        return "ChangeData{" +
                "op='" + op + '\'' +
                ", table='" + table + '\'' +
                ", database='" + database + '\'' +
                ", before=" + before +
                ", after=" + after +
                '}';
    }
}
