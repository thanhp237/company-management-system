package com.group3.company_management.core.dto;

// src/main/java/com/group3/company_management/core/dto/CustomerResponse.java



import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for customer response
 * Same pattern as UserResponse
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {
    
    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private String customerSource;
    private Long assignedSalesId;
    private String customerStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}