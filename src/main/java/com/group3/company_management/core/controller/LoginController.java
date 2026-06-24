package com.group3.company_management.core.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handle login/logout pages and redirects
 */
@Controller
public class LoginController {
    
    /**
     * GET /login
     * Display login page
     */
    @GetMapping("/login")
    public String showLoginPage(Model model, 
                                @RequestParam(required = false) String error,
                                @RequestParam(required = false) String logout,
                                @RequestParam(required = false) String locked,
                                @RequestParam(required = false) String inactive) {
        model.addAttribute("title", "Login");
        
        if (error != null) {
            model.addAttribute("error", true);
        }
        if (logout != null) {
            model.addAttribute("logout", true);
        }
        if (locked != null) {
            model.addAttribute("locked", true);
        }
        if (inactive != null) {
            model.addAttribute("inactive", true);
        }
        
        return "auth/login"; // customuserdetailsservice will get the username and password from the form and authenticate before redirecting to LoginController
    }
    
    /**
     * GET /
     * Redirect root to dashboard or login
     */
    @GetMapping("/auth")
    public String redirectRoot(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }
    
    /**
     * GET /forgot-password
     * Display forgot password page (placeholder)
     * Combined with future implementation of password reset/change functionality
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        model.addAttribute("title", "Forgot Password");
        return "auth/forgot-password";
    }
}
