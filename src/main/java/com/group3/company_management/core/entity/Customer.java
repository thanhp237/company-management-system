package com.group3.company_management.core.entity;
// src/main/java/com/group3/company_management/core/entity/Customer.java

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Customer entity - same pattern as User entity
 * Soft delete enabled with @SQLDelete and @SQLRestriction
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
public class Customer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 255)
    private String fullName;
    
    @Column(nullable = false, unique = true, length = 20)
    private String phone;
    
    @Column(length = 255)
    private String email;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    @Column(length = 100)
    private String customerSource;
    
    @Column(name = "assigned_sales_id")
    private Long assignedSalesId;
    
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String customerStatus = "ACTIVE";
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;
    
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
}