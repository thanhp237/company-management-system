package com.group3.company_management.core.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.group3.company_management.core.dto.LoginRequest;
import com.group3.company_management.core.dto.LoginResponse;
import com.group3.company_management.core.entity.LoginAttempt;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.LoginAttemptRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handle user login with:
 * - Credential validation
 * - Failed attempt tracking
 * - Account locking after N failed attempts
 * - JWT token generation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${app.auth.max-failed-attempts:5}")
    private int maxFailedAttempts;
    
    @Value("${app.auth.lock-duration-minutes:15}")
    private long lockDurationMinutes;
    
    /**
     * Main login method
     * @param request login credentials
     * @param ipAddress client IP address
     * @param userAgent client user agent
     * @return JWT tokens on success
     */
    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        try {
            // Step 1: Find user
            User user = userRepository.findByUsernameAndNotDeleted(request.getUsername())
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
            
            // Step 2: Check if account is locked
            if (!user.isAccountNonLocked()) {
                log.warn("⛔ Login attempt on LOCKED account: {} from IP: {}", 
                    user.getUsername(), ipAddress);
                throw new BadCredentialsException("Account is locked. Please contact administrator.");
            }
            
            // Step 3: Check if account is active
            if (!user.isEnabled()) {
                log.warn("⛔ Login attempt on INACTIVE account: {} from IP: {}", 
                    user.getUsername(), ipAddress);
                throw new BadCredentialsException("Account is inactive.");
            }
            
            // Step 4: Authenticate password
            try {
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getUsername(),
                                request.getPassword()
                        )
                );
            } catch (AuthenticationException ex) {
                // Password is wrong - handle failed login
                handleFailedLogin(user, ipAddress, userAgent);
                throw new BadCredentialsException("Invalid credentials");
            }
            
            // Step 5: Authentication successful - reset failed attempts
            user.setFailedAttempts(0);
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            
            // Step 6: Log successful login attempt
            loginAttemptRepository.save(LoginAttempt.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .status("SUCCESS")
                    .build());
            
            log.info("✅ User {} logged in successfully from IP: {}", user.getUsername(), ipAddress);
            
            // Step 7: Generate JWT tokens
            String accessToken = jwtTokenProvider.generateAccessToken(
                    user.getId(), user.getUsername(), user.getEmail());
            String refreshToken = jwtTokenProvider.generateRefreshToken(
                    user.getId(), user.getUsername());
            
            // Step 8: Return response with tokens
            return LoginResponse.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .avatarUrl(user.getAvatarUrl())
                    .departmentId(user.getDepartmentId())
                    .roleId(user.getRole() == null ? null : user.getRole().getId())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .build();
            
        } catch (BadCredentialsException ex) {
            throw ex;
        }
    }
    
    /**
     * Handle failed login attempt
     * - Increment failed attempt counter
     * - Lock account after N failed attempts
     * - Log attempt for audit trail
     */
    @Transactional
    private void handleFailedLogin(User user, String ipAddress, String userAgent) {
        // Increment failed attempts
        int newFailCount = user.getFailedAttempts() + 1;
        user.setFailedAttempts(newFailCount);
        
        // Lock account if max failed attempts reached
        if (newFailCount >= maxFailedAttempts) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
            log.warn("🔒 User account LOCKED due to {} failed attempts: {}", 
                newFailCount, user.getUsername());
        }
        
        userRepository.save(user);
        
        // Log failed attempt
        loginAttemptRepository.save(LoginAttempt.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status("FAILED")
                .build());
        
        log.warn("❌ Failed login attempt for user: {} (attempt #{}) from IP: {}", 
            user.getUsername(), newFailCount, ipAddress);
    }
    @Transactional
    public void logout(String username, String ipAddress, String userAgent) {
        // Tìm user xem có tồn tại không
        User user = userRepository.findByUsernameAndNotDeleted(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        // Lưu log ghi nhận user đã đăng xuất thành công
        loginAttemptRepository.save(LoginAttempt.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .status("LOGOUT") // Đánh dấu trạng thái là LOGOUT
                .build());

        log.info("🚪 User {} logged out successfully from IP: {}", user.getUsername(), ipAddress);
    }
}
