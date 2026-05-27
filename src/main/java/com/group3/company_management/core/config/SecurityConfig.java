package com.group3.company_management.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.group3.company_management.core.security.JwtAuthenticationEntryPoint;
import com.group3.company_management.core.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security configuration for JWT-based authentication
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    /**
     * Password encoder using BCrypt algorithm
     * Hashes passwords securely before storing in database
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * Authentication manager for validating credentials
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    
    /**
     * HTTP security filter chain
     * Configures:
     * - CSRF disabled (stateless REST API)
     * - Session management (stateless with JWT)
     * - Public endpoints (login, health check)
     * - Protected endpoints (require JWT token)
     * - JWT filter for token validation
     */
    @Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

    http

            // Disable CSRF for JWT/API usage
            .csrf(csrf -> csrf.disable())

            // Disable default Spring login page
            .formLogin(form -> form.disable())

            // Disable HTTP Basic auth popup
            .httpBasic(httpBasic -> httpBasic.disable())

            // JWT unauthorized handler
            .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )

            // Stateless session for JWT
            .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Route permissions
            .authorizeHttpRequests(authz -> authz

                    // Public API endpoints
                    .requestMatchers(
                            HttpMethod.POST,
                            "/api/v1/auth/login"
                    ).permitAll()

                    .requestMatchers(
                            HttpMethod.GET,
                            "/api/v1/auth/health"
                    ).permitAll()

                    // Public UI pages
                    .requestMatchers(
                            "/",
                            "/login",
                            "/auth",
                            "/forgot-password",
                            "/css/**",
                            "/js/**",
                            "/images/**"
                    ).permitAll()

                    // Everything else requires auth
                    .anyRequest().authenticated()
            )

            // JWT filter
            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

    return http.build();
}
}