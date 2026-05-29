
// src/main/java/com/group3/company_management/customer/dto/UpdateCustomerRequest.java
package com.group3.company_management.core.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCustomerRequest {
    
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255, message = "Full name must be between 2-255 characters")
    private String fullName;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;
    
    @Size(max = 100, message = "Customer source must not exceed 100 characters")
    private String customerSource;
    
    @Min(value = 1, message = "Invalid sales person ID")
    private Long assignedSalesId;
    
    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be ACTIVE or INACTIVE")
    private String customerStatus;
}