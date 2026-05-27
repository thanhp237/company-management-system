package com.group3.company_management.core.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.group3.company_management.core.dto.LoginRequest;
import com.group3.company_management.core.dto.LoginResponse;
import com.group3.company_management.core.service.AuthenticationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST API endpoints for user authentication
 * Handles:
 * - User login with JWT token generation
 * - Session health check
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {
    
    private final AuthenticationService authenticationService;
    
    /**
     * POST /api/v1/auth/login
     * User login endpoint
     * 
     * @param request username and password
     * @param httpRequest HTTP request context (for IP, user-agent)
     * @return JWT tokens on success
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        LoginResponse response = authenticationService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }
    
    /**
     * POST /api/v1/auth/logout
     * Logout endpoint (client removes token from storage)
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok().body(
            Map.of("message", "Logged out successfully")
        );
    }
    
    /**
     * GET /api/v1/auth/health
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body(
            Map.of("status", "Auth service is healthy")
        );
    }
    
    /**
     * Extract client IP address from request
     * Handles X-Forwarded-For header for proxies
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}