package com.group3.company_management.core.controller;

// src/main/java/com/group3/company_management/core/controller/CustomerAuthController.java

import com.group3.company_management.customer.dto.CustomerLoginRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Customer authentication controller
 * Separate login for customers (not employees)
 * Spring Security 6+ with email-based authentication
 */
@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
@Slf4j
public class CustomerAuthController {
    
    private final AuthenticationManager customerAuthenticationManager;
    
    /**
     * GET /customer/login
     * Display customer login page (separate from /login for employees)
     */
    @GetMapping("/select-login")
    public String showCustomerLoginPage(
            Model model,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Authentication authentication) {
        
        // Redirect if already authenticated
        if (authentication != null && authentication.isAuthenticated()) {
            log.info("✅ Authenticated customer redirected from login");
            return "redirect:/customer/portal";
        }
        
        model.addAttribute("title", "Customer Login");
        
        if (error != null) {
            model.addAttribute("error", "Invalid email or password");
            log.warn("⚠️ Customer login failed - invalid credentials");
        }
        
        if (logout != null) {
            model.addAttribute("logout", "You have been logged out successfully");
            log.info("ℹ️ Customer logged out");
        }
        
       return "auth/customer-login";
    }
}