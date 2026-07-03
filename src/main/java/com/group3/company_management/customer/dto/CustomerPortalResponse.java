package com.group3.company_management.customer.dto;

// src/main/java/com/group3/company_management/customer/dto/CustomerPortalResponse.java

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for customer portal dashboard info
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerPortalResponse {
    
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String customerStatus;
    
    // Portal metrics
    private Long contractCount;
    private Long pendingQuotesCount;
    private Long totalPaidAmount;
}