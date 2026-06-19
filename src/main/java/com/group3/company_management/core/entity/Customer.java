package com.group3.company_management.core.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Id;

import java.time.LocalDateTime;
@Getter
@Setter
@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long createdBy;

    private String name;

    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;
    @Column(name = "assigned_sales_id")
    private Long assignedSalesId;

    private LocalDateTime createdAt;

    private String customerSource;
    @Column(name = "customer_status")
    private String customerStatus;

    private LocalDateTime deletedAt;

    private String fullName;

    private Boolean isDeleted;

    private String phone;

    private LocalDateTime updatedAt;

    private String customerType;

    private String taxCode;

    private String companyName;

    private Long ownerId;

    private Integer failedAttempts;

    private LocalDateTime lastLoginAt;

    private LocalDateTime lockedUntil;

    private String passwordHash;
    public Customer() {
    }
}
