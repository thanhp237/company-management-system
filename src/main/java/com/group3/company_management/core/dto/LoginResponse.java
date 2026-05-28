package com.group3.company_management.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body after successful login
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private Long departmentId;
    private Long roleId;
    private String accessToken;      // JWT token for API requests
    private String refreshToken;     // JWT token for refreshing access token
    private String tokenType;        // "Bearer"
}




