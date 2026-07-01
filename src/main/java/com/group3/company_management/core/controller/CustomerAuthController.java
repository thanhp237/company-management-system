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
        
        model.addAttribute("title", "Đăng nhập khách hàng");
        model.addAttribute("loginMode", "customer");
        
        if (error != null) {
            model.addAttribute("customerError", "Email hoặc mật khẩu không đúng.");
            log.warn("⚠️ Customer login failed - invalid credentials");
        }
        
        if (logout != null) {
            model.addAttribute("customerLogout", "Bạn đã đăng xuất thành công.");
            log.info("ℹ️ Customer logged out");
        }
        
       return "auth/login";
    }

    @PostMapping("/login")
    public String loginCustomer(
            @ModelAttribute CustomerLoginRequest request,
            HttpServletRequest httpRequest,
            Model model) {
        try {
            Customer customer = customerRepository.findByEmailAndNotDeleted(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng."));

            if (!customer.isEnabled()) {
                throw new RuntimeException("Tài khoản khách hàng chưa được kích hoạt.");
            }

            if (!customer.isAccountNonLocked()) {
                throw new RuntimeException("Tài khoản khách hàng đang bị khóa.");
            }

            if (customer.getPasswordHash() == null
                    || !passwordEncoder.matches(request.getPassword(), customer.getPasswordHash())) {
                throw new RuntimeException("Email hoặc mật khẩu không đúng.");
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
            model.addAttribute("title", "Đăng nhập khách hàng");
            model.addAttribute("loginMode", "customer");
            model.addAttribute("customerError", exception.getMessage());
            log.warn("⚠️ Customer login failed: {}", exception.getMessage());
            return "auth/login";
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
