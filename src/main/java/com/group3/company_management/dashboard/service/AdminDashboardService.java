package com.group3.company_management.dashboard.service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.group3.company_management.core.dto.SystemMetricsDto;
import com.group3.company_management.core.repository.LoginAttemptRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.sun.management.OperatingSystemMXBean;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service for admin dashboard metrics and system health
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {
    
    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final JdbcTemplate jdbcTemplate;
    
    @Autowired(required = false)
    private DataSource dataSource;
    
    private static final long START_TIME = System.currentTimeMillis();
    
    /**
     * Get comprehensive system metrics
     */
    public SystemMetricsDto getSystemMetrics() {
        try {
            return SystemMetricsDto.builder()
                    // System Status
                    .systemStatus(calculateSystemStatus())
                    .lastCheckedAt(LocalDateTime.now())
                    .uptime(calculateUptime())
                    
                    // Performance
                    .cpuUsage(getCpuUsage())
                    .memoryUsage(getMemoryUsage())
                    .diskUsage(getDiskUsage())
                    
                    // Database
                    .activeConnections(getActiveConnections())
                    .maxConnections(getMaxConnections())
                    .databaseSize(getDatabaseSize())
                    .queryResponseTime(getQueryResponseTime())
                    
                    // API
                    .totalRequests(getTotalRequests())
                    .successfulRequests(getSuccessfulRequests())
                    .failedRequests(getFailedRequests())
                    .errorRate(calculateErrorRate())
                    .avgResponseTime(getAvgResponseTime())
                    
                    // User
                    .totalActiveUsers(getTotalActiveUsers())
                    .totalInactiveUsers(getTotalInactiveUsers())
                    .activeSessionsCount(getActiveSessionsCount())
                    .lastLoginCount(getLastLoginCount())
                    
                    // Transaction
                    .totalTransactions(getTotalTransactions())
                    .failedTransactions(getFailedTransactions())
                    .successRate(calculateTransactionSuccessRate())
                    
                    // Security
                    .failedLoginAttempts(getFailedLoginAttempts())
                    .lockedAccounts(getLockedAccountsCount())
                    .deletedUsersCount(getDeletedUsersCount())
                    
                    .build();
        } catch (Exception e) {
            log.error("Error collecting system metrics", e);
            return buildDefaultMetrics();
        }
    }
    
    /**
     * Calculate overall system status
     */
    private Double getCpuUsage() {
    OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    double cpuLoad = osBean.getProcessCpuLoad();

    return cpuLoad < 0 ? 0 : Math.round(cpuLoad * 10000.0) / 100.0;
}
    private String calculateSystemStatus() {
        Double cpuUsage = getCpuUsage();
        Double memoryUsage = getMemoryUsage();
        Double errorRate = calculateErrorRate();
        
        if (cpuUsage > 90 || memoryUsage > 90 || errorRate > 10) {
            return "CRITICAL";
        } else if (cpuUsage > 70 || memoryUsage > 70 || errorRate > 5) {
            return "WARNING";
        }
        return "HEALTHY";
    }
    
    /**
     * Calculate uptime duration
     */
    private String calculateUptime() {
        long uptime = System.currentTimeMillis() - START_TIME;
        long seconds = (uptime / 1000) % 60;
        long minutes = (uptime / (1000 * 60)) % 60;
        long hours = (uptime / (1000 * 60 * 60)) % 24;
        long days = (uptime / (1000 * 60 * 60 * 24));
        
        return String.format("%d days, %d hours, %d minutes", days, hours, minutes);
    }
    
    /**
     * Get CPU usage percentage
     */

    /**
     * Get memory usage percentage
     */
    private Double getMemoryUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        return ((double) heapUsed / heapMax) * 100;
    }
    
    /**
     * Get disk usage percentage (placeholder)
     */
    private Double getDiskUsage() {
        // This would require file system access
        // Returning mock data for now
        return 45.5;
    }
    
    /**
     * Get active database connections
     */
    private Integer getActiveConnections() {
        try {
            String sql = "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'";
            Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
            return result != null ? result : 0;
        } catch (Exception e) {
            log.warn("Could not get active connections", e);
            return 0;
        }
    }
    
    /**
     * Get max allowed connections
     */
    private Integer getMaxConnections() {
        try {
            String sql = "SHOW max_connections";
            String result = jdbcTemplate.queryForObject(sql, String.class);
            return Integer.parseInt(result);
        } catch (Exception e) {
            log.warn("Could not get max connections", e);
            return 100;
        }
    }
    
    /**
     * Get database size in bytes
     */
    private Long getDatabaseSize() {
        try {
            String sql = "SELECT pg_database_size(current_database())";
            Long size = jdbcTemplate.queryForObject(sql, Long.class);
            return size != null ? size : 0L;
        } catch (Exception e) {
            log.warn("Could not get database size", e);
            return 0L;
        }
    }
    
    /**
     * Get average query response time (mock)
     */
    private Double getQueryResponseTime() {
        return 25.5; // milliseconds - replace with actual query monitoring
    }
    
    /**
     * Get total API requests (mock)
     */
    private Long getTotalRequests() {
        return 10500L;
    }
    
    /**
     * Get successful API requests (mock)
     */
    private Long getSuccessfulRequests() {
        return 10200L;
    }
    
    /**
     * Get failed API requests (mock)
     */
    private Long getFailedRequests() {
        return 300L;
    }
    
    /**
     * Calculate error rate
     */
    private Double calculateErrorRate() {
        Long total = getTotalRequests();
        Long failed = getFailedRequests();
        return total > 0 ? (double) (failed * 100) / total : 0.0;
    }
    
    /**
     * Get average response time (mock)
     */
    private Double getAvgResponseTime() {
        return 145.8; // milliseconds
    }
    
    /**
     * Get total active users
     */
    private Integer getTotalActiveUsers() {
        try {
            String sql = "SELECT COUNT(*) FROM system_accounts WHERE status = 'ACTIVE' AND is_deleted = false";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Could not get active users count", e);
            return 0;
        }
    }
    
    /**
     * Get total inactive users
     */
    private Integer getTotalInactiveUsers() {
        try {
            String sql = "SELECT COUNT(*) FROM system_accounts WHERE status = 'INACTIVE' AND is_deleted = false";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Could not get inactive users count", e);
            return 0;
        }
    }
    
    /**
     * Get active sessions (users logged in last 30 minutes)
     */
    private Integer getActiveSessionsCount() {
        try {
            String sql = "SELECT COUNT(DISTINCT user_id) FROM login_attempts " +
                    "WHERE status = 'SUCCESS' " +
                    "AND attempt_at > NOW() - INTERVAL '30 minutes'";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Could not get active sessions", e);
            return 0;
        }
    }
    
    /**
     * Get logins in last 24 hours
     */
    private Long getLastLoginCount() {
        try {
            String sql = "SELECT COUNT(*) FROM login_attempts " +
                    "WHERE status = 'SUCCESS' " +
                    "AND attempt_at > NOW() - INTERVAL '24 hours'";
            Long count = jdbcTemplate.queryForObject(sql, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.warn("Could not get last login count", e);
            return 0L;
        }
    }
    
    /**
     * Get total transactions (mock)
     */
    private Long getTotalTransactions() {
        return 5420L;
    }
    
    /**
     * Get failed transactions (mock)
     */
    private Long getFailedTransactions() {
        return 150L;
    }
    
    /**
     * Calculate transaction success rate
     */
    private Double calculateTransactionSuccessRate() {
        Long total = getTotalTransactions();
        Long failed = getFailedTransactions();
        return total > 0 ? ((double) (total - failed) * 100) / total : 0.0;
    }
    
    /**
     * Get failed login attempts in last 24 hours
     */
    private Integer getFailedLoginAttempts() {
        try {
            String sql = "SELECT COUNT(*) FROM login_attempts " +
                    "WHERE status = 'FAILED' " +
                    "AND attempt_at > NOW() - INTERVAL '24 hours'";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Could not get failed login attempts", e);
            return 0;
        }
    }
    
    /**
     * Get locked accounts
     */
    private Integer getLockedAccountsCount() {
        try {
            String sql = "SELECT COUNT(*) FROM system_accounts " +
                    "WHERE locked_until IS NOT NULL " +
                    "AND locked_until > NOW()";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Could not get locked accounts", e);
            return 0;
        }
    }
    
    /**
     * Get soft-deleted users
     */
    private Integer getDeletedUsersCount() {
        try {
            String sql = "SELECT COUNT(*) FROM system_accounts WHERE is_deleted = true";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.warn("Could not get deleted users count", e);
            return 0;
        }
    }
    
    /**
     * Build default metrics when errors occur
     */
    private SystemMetricsDto buildDefaultMetrics() {
        return SystemMetricsDto.builder()
                .systemStatus("UNKNOWN")
                .lastCheckedAt(LocalDateTime.now())
                .uptime("N/A")
                .cpuUsage(0.0)
                .memoryUsage(0.0)
                .diskUsage(0.0)
                .activeConnections(0)
                .maxConnections(100)
                .databaseSize(0L)
                .queryResponseTime(0.0)
                .totalRequests(0L)
                .successfulRequests(0L)
                .failedRequests(0L)
                .errorRate(0.0)
                .avgResponseTime(0.0)
                .totalActiveUsers(0)
                .totalInactiveUsers(0)
                .activeSessionsCount(0)
                .lastLoginCount(0L)
                .totalTransactions(0L)
                .failedTransactions(0L)
                .successRate(0.0)
                .failedLoginAttempts(0)
                .lockedAccounts(0)
                .deletedUsersCount(0)
                .build();
    }
}