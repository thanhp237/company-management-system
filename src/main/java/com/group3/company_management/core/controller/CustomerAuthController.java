package com.group3.company_management.core.controller;

// src/main/java/com/group3/company_management/core/controller/CustomerAuthController.java

import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.repository.CustomerRepository;
import com.group3.company_management.customer.dto.CustomerLoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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
    
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * GET /customer/login
     * Display customer login page (separate from /login for employees)
     */
    @GetMapping({"/login", "/select-login"})
    public String showCustomerLoginPage(
            Model model,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Authentication authentication) {
        
        // Redirect if already authenticated
        if (authentication != null && authentication.getPrincipal() instanceof Customer) {
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

    @PostMapping("/login")
    public String loginCustomer(
            @ModelAttribute CustomerLoginRequest request,
            HttpServletRequest httpRequest,
            Model model) {
        try {
            Customer customer = customerRepository.findByEmailAndNotDeleted(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Invalid email or password"));

            if (!customer.isEnabled()) {
                throw new RuntimeException("Customer account is inactive");
            }

            if (!customer.isAccountNonLocked()) {
                throw new RuntimeException("Customer account is locked");
            }

            if (customer.getPasswordHash() == null
                    || !passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
                throw new RuntimeException("Invalid email or password");
            }

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    customer,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

            log.info("✅ Customer logged in: {}", customer.getEmail());
            return "redirect:/dashboard/customer";
        } catch (RuntimeException exception) {
            model.addAttribute("title", "Customer Login");
            model.addAttribute("error", exception.getMessage());
            log.warn("⚠️ Customer login failed: {}", exception.getMessage());
            return "auth/customer-login";
        }
    }

    @GetMapping("/logout")
    public String logoutCustomer(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/customer/login?logout";
    }
}
