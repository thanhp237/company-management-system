package com.group3.company_management.core.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW(), is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class User implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;
    
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    
    @Column(nullable = false, length = 255)
    private String fullName;

    // ========== NEW: AUTHENTICATION FIELDS FOR LOGIN ==========
    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 50)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "role_id")
    private Long roleId;

    // ========== NEW: ACCOUNT SECURITY FIELDS ==========
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE"; // PENDING, ACTIVE, LOCKED, DISABLED

    @Column(nullable = false)
    private Integer failedAttempts = 0;  // Track failed login attempts

    @Column
    private LocalDateTime lockedUntil;   // Account lock timestamp

    @Column
    private LocalDateTime lastLoginAt;   // Last successful login

    // ========== NEW: TIMESTAMPS ==========
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ========== EXISTING: SOFT DELETE ==========
    private boolean isDeleted = false;
    private LocalDateTime deletedAt;

    // ========== JPA LIFECYCLE CALLBACKS ==========
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ========== USERDETAILS IMPLEMENTATION (SPRING SECURITY) ==========
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return "ACTIVE".equals(this.status) && 
               (this.lockedUntil == null || LocalDateTime.now().isAfter(this.lockedUntil));
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(this.status) && !this.isDeleted;
    }
}