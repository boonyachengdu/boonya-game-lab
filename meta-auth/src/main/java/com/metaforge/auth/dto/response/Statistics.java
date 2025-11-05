package com.metaforge.auth.dto.response;

import lombok.Data;

@Data
public class Statistics {
    private Long totalUsers = 0L;
    private Long activeUsers = 0L;
    private Long totalRoles = 0L;
    private Long todayLogins = 0L;
}
