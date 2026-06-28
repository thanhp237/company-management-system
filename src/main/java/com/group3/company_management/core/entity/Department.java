package com.group3.company_management.core.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "departments")
@Getter
@Setter

public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Code id required")
    private String code;
    @NotBlank(message = "Code id required")
    private String name;
    private Boolean isDeleted = false;
    private String description;

    @Column(name = "manager_id")
    private Long managerId;

    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
    private String deletedBy;
    private String status = "ACTIVE";
    public Department() {

    }

    public Department(Long id, String code, String name,String status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.status = status;
    }

    public Department(Long id, String code, String name, Boolean isDeleted, LocalDateTime deletedAt, String deletedBy, String status) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.isDeleted = isDeleted;
        this.deletedAt = deletedAt;
        this.deletedBy = deletedBy;
        this.status = status;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (isDeleted == null) isDeleted = false;
        if (status == null || status.trim().isEmpty()) status = "ACTIVE";
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
