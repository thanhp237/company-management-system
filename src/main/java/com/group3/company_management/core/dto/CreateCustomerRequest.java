package com.group3.company_management.core.dto;

// src/main/java/com/group3/company_management/customer/dto/CreateCustomerRequest.java
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
public class CreateCustomerRequest {
    
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 255, message = "Full name must be between 2-255 characters")
    private String fullName;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+\\-\\s()]{7,20}$", message = "Invalid phone number format")
    private String phone;
    
    @Email(message = "Invalid email format")
    private String email;
    
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;
    
    @Size(max = 100, message = "Customer source must not exceed 100 characters")
    private String customerSource;
    
    @Min(value = 1, message = "Invalid sales person ID")
    private Long assignedSalesId;
}