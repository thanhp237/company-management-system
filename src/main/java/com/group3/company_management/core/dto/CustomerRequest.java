package com.group3.company_management.core.dto;

// src/main/java/com/group3/company_management/core/dto/CustomerRequest.java



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for customer request (create/update)
 * Same pattern as UserRequest
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerRequest {
    
    private Long id;
    private String fullName;
    private String phone;
    private String email;
    private String address;
    private String customerSource;
    private Long assignedSalesId;
    private String customerStatus;
}