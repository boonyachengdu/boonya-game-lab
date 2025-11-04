package com.metaforge.bigdata.trino.controller;

import com.metaforge.bigdata.trino.service.PrestoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/presto")
public class PrestoController {

    @Autowired
    private PrestoService prestoService;

    @GetMapping("/query")
    public ResponseEntity<?> queryData(@RequestParam String sql,
                                       @RequestParam(defaultValue = "100") int limit) {
        try {
            List<Map<String, Object>> result = prestoService.executeQuery(sql, limit);
            return ResponseEntity.ok(result);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/tables")
    public ResponseEntity<?> listTables() {
        try {
            List<String> tables = prestoService.listTables();
            return ResponseEntity.ok(Collections.singletonMap("tables", tables));
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @GetMapping("/test-connection")
    public ResponseEntity<?> testConnection() {
        boolean isConnected = prestoService.testConnection();
        return ResponseEntity.ok(Collections.singletonMap("connected", isConnected));
    }
}