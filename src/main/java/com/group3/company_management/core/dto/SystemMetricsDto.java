package com.group3.company_management.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for system metrics and health status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemMetricsDto {
    
    // System Status
    private String systemStatus;           // HEALTHY, WARNING, CRITICAL
    private LocalDateTime lastCheckedAt;
    private String uptime;                 // e.g., "15 days, 3 hours"
    
    // Performance Metrics
    private Double cpuUsage;               // 0-100
    private Double memoryUsage;            // 0-100
    private Double diskUsage;              // 0-100
    
    // Database Metrics
    private Integer activeConnections;
    private Integer maxConnections;
    private Long databaseSize;             // in bytes
    private Double queryResponseTime;      // in milliseconds
    
    SystemMetricsDto dto = SystemMetricsDto.builder().build();
    
    // API Metrics
    private Long totalRequests;
    private Long successfulRequests;
    private Long failedRequests;
    private Double errorRate;              // percentage
    private Double avgResponseTime;        // in milliseconds
    
    // User Metrics
    private Integer totalActiveUsers;
    private Integer totalInactiveUsers;
    private Integer activeSessionsCount;
    private Long lastLoginCount;           // logins in last 24 hours
    
    // Transaction Metrics
    private Long totalTransactions;
    private Long failedTransactions;
    private Double successRate;            // percentage
    
    // Security Metrics
    private Integer failedLoginAttempts;
    private Integer lockedAccounts;
    private Integer deletedUsersCount;
}