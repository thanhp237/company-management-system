// package com.group3.company_management.customer.config;

// // src/main/java/com/group3/company_management/customer/config/CustomerSecurityConfig.java

// import com.group3.company_management.customer.security.CustomerUserDetailsService;
// import lombok.RequiredArgsConstructor;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
// import org.springframework.security.crypto.password.PasswordEncoder;

// /**
//  * Spring Security 6+ configuration for Customer authentication
//  * Uses same PasswordEncoder as User (Employee) system
//  */
// @Configuration
// @RequiredArgsConstructor
// public class CustomerSecurityConfig {
    
//     private final CustomerUserDetailsService customerUserDetailsService;
//     private final PasswordEncoder passwordEncoder;
    
//     /**
//      * Authentication provider for customers
//      */
//     @Bean("customerAuthenticationProvider")
//     public DaoAuthenticationProvider customerAuthenticationProvider() {
//         DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
//         provider.setUserDetailsService(customerUserDetailsService);
//         provider.setPasswordEncoder(passwordEncoder);
//         return provider;
//     }
// }