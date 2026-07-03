package com.group3.company_management.core.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for login endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "Vui lòng nhập tên đăng nhập")
    private String username;
    
    @NotBlank(message = "Vui lòng nhập mật khẩu")
    private String password;
}
