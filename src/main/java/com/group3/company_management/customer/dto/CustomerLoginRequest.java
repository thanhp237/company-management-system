package com.group3.company_management.customer.dto;

// src/main/java/com/group3/company_management/customer/dto/CustomerLoginRequest.java

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for customer login request
 * Email-based login (different from User username-based)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerLoginRequest {
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;
    
    @NotBlank(message = "Password is required")
    private String password;
}