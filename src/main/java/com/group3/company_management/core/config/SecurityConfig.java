package com.group3.company_management.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import com.group3.company_management.core.security.CustomLoginSuccessHandler;
import com.group3.company_management.core.security.JwtAuthenticationEntryPoint;
import com.group3.company_management.core.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

/**
 * Spring Security configuration for UI form login and JWT-based API
 * authentication
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomLoginSuccessHandler customLoginSuccessHandler;
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
     * - CSRF disabled for simple HTML forms and JWT API requests
     * - Form login for Thymeleaf UI pages
     * - Public endpoints (login, static assets, auth API)
     * - Protected UI pages (require session authentication)
     * - JWT filter for API token validation
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http

                // Disable CSRF for simple HTML forms and JWT/API usage
                .csrf(csrf -> csrf.disable())
                               
              

                // Use the custom Thymeleaf login page for browser users
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(customLoginSuccessHandler)
                        .failureUrl("/login?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                // Disable HTTP Basic auth popup
                .httpBasic(httpBasic -> httpBasic.disable())

                // Return API-style unauthorized responses for API calls only
                .exceptionHandling(exception -> exception
                        .defaultAuthenticationEntryPointFor(
                                jwtAuthenticationEntryPoint,
                                new AntPathRequestMatcher("/api/**")
                        )
                )

                // UI login needs a session; JWT API requests still authenticate via token
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
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
                                                                "/api/v1/auth/health")
                                                .permitAll()
                                                .requestMatchers("/employees/**")
                                                .hasAnyRole("MANAGER", "ADMIN", "SALES", "ADMINOFFICER")

                        // Public UI pages
                        .requestMatchers(
                                "/",
                                "/login",
                                "/auth",
                                "/forgot-password",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/webjars/**",
                                "/main.css"
                        ).permitAll()
                        .requestMatchers(
                                "/dashboard/**",
                                "/users/**",
                                "/departments/**",
                                "/change-password",
                                "/first-change-password"
                        ).authenticated()

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
