

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

/**
 * Customer entity - B2B customers who can login and access portal
 * Implements UserDetails for Spring Security 6+ authentication
 * Different from User (Employee) entity
 */
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customers_phone", columnList = "phone"),
    @Index(name = "idx_customers_email", columnList = "email"),
    @Index(name = "idx_customers_status", columnList = "customer_status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE customers SET deleted_at = CURRENT_TIMESTAMP, is_deleted = true WHERE id = ?")
@SQLRestriction("is_deleted = false")
public class Customer implements UserDetails {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String fullName;
    
    @Column(nullable = false, unique = true, length = 20)
    private String phone;
    
    /**
     * Email is username for customer login (unique)
     */
    @Column(nullable = false, unique = true, length = 255)
    private String email;
    
    /**
     * Password hash for authentication (same as User)
     */
    @Column(length = 255)
    private String passwordHash;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    @Column(length = 100)
    private String customerSource;
    
    @Column(name = "assigned_sales_id")
    private Long assignedSalesId;
    
    /**
     * Customer Status: ACTIVE or INACTIVE
     */
    @Column(nullable = false, length = 20)
    private String customerStatus;
    
    /**
     * Failed login attempts (for account lockout like User)
     */
    @Column(name = "failed_attempts")
    @Builder.Default
    private Integer failedAttempts = 0;
    
    /**
     * Account locked until timestamp
     */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    
    /**
     * Last login timestamp
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(length = 100)
    private String customerType;

    @Column(length = 50)
    private String taxCode;

    @Column(length = 255)
    private String companyName;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(length = 255)
    private String name;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    @Column(length = 20)
    private String opportunityLevel;

    @Column(columnDefinition = "TEXT")
    private String evaluationNote;

    @Column(length = 10)
    private String gender;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // ============= Spring Security 6+ UserDetails Implementation =============
    
    /**
     * Customers have no special roles - all equal access
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }
    
    /**
     * Email is the username for customer login
     */
    @Override
    public String getUsername() {
        return this.email;
    }
    
    @Override
    public String getPassword() {
        return this.passwordHash;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    /**
     * Account is locked if lockedUntil is in the future
     */
    @Override
    public boolean isAccountNonLocked() {
        if (this.lockedUntil == null) {
            return true;
        }
        return LocalDateTime.now().isAfter(this.lockedUntil);
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    /**
     * Customer is enabled if status is ACTIVE and not deleted
     */
    @Override
    public boolean isEnabled() {
        return "ACTIVE".equals(this.customerStatus) && !this.isDeleted;
    }
}

