
// src/main/java/com/group3/company_management/customer/dto/CustomerResponse.java

package com.group3.company_management.core.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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