package com.group3.company_management.core.entity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "system_accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE system_accounts SET deleted_at = CURRENT_TIMESTAMP, is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "group_id")
    private Long groupId;

    @Column(name = "role_id", insertable = false, updatable = false)
    private Long roleId;

    /**
     * Status: ACTIVE or INACTIVE (only 2 statuses)
     * - ACTIVE: User can log in
     * - INACTIVE: User cannot log in
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "first_login")
    @Builder.Default
    private Boolean firstLogin = true;
    /**
     * Track failed login attempts
     * Used for account security and temporary locking via lockedUntil timestamp
     */
    @Column(name = "failed_attempts")
    @Builder.Default
    private Integer failedAttempts = 0;

    /**
     * Temporary account lock timestamp
     * Account is locked if current time is before this timestamp
     * Automatically unlocks after lock duration expires
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + role.getRoleCode()));
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Check if account is not locked (can log in)
     * Returns true if:
     * - lockedUntil is null OR
     * - current time is after lockedUntil (lock has expired)
     */
    @Override
    public boolean isAccountNonLocked() {
        return this.lockedUntil == null || LocalDateTime.now().isAfter(this.lockedUntil);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Check if user is enabled (can log in)
     * Returns true if:
     * - status is ACTIVE AND
     * - not soft-deleted
     */
    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(this.status) && !this.isDeleted;
    }

    public boolean isActive() {
        return "ACTIVE".equals(this.status) && !this.isDeleted;
    }

    public boolean isInactive() {
        return "INACTIVE".equals(this.status) && !this.isDeleted;
    }

    public boolean isAdmin() {
        return role != null &&
                "ADMIN".equalsIgnoreCase(role.getRoleCode());
    }

    public boolean isManager() {
        return role != null &&
                "MANAGER".equalsIgnoreCase(role.getRoleCode());
    }

    public boolean isSales() {
        return role != null &&
                "SALES".equalsIgnoreCase(role.getRoleCode());
    }

}
