package com.group3.company_management.core.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Track all login attempts (successful and failed) for security audit trail
 * 
 * Used to implement:
 * - Failed attempt tracking
 * - Account locking after N failed attempts
 * - Security audit logs
 * - Failed login analysis
 */
@Entity
@Table(name = "login_attempts", indexes = {
    @Index(name = "idx_login_attempts_user_id", columnList = "user_id"),
    @Index(name = "idx_login_attempts_username", columnList = "username"),
    @Index(name = "idx_login_attempts_status", columnList = "status"),
    @Index(name = "idx_login_attempts_attempt_at", columnList = "attempt_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Reference to the user
     * Can be null if login attempt with invalid username
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Username attempted to login with
     */
    @Column(name = "username", nullable = false, length = 100)
    private String username;

    /**
     * Client IP address (for security analysis)
     */
    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    /**
     * Client user agent (browser/app info)
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Status of login attempt: "SUCCESS" or "FAILED"
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * Timestamp when login attempt occurred
     * Set automatically at creation via @PrePersist
     */
    @Column(name = "attempt_at", nullable = false, updatable = false)
    private LocalDateTime attemptAt;

    /**
     * Automatically set attemptAt before persisting to database
     */
    @PrePersist
    protected void onCreate() {
        if (this.attemptAt == null) {
            this.attemptAt = LocalDateTime.now();
        }
    }
}
