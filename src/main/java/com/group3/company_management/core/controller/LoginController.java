package com.group3.company_management.core.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {
    
    @GetMapping("/login")
    public String showLoginPage(Model model,
                                @RequestParam(required = false) String error,
                                @RequestParam(required = false) String locked,
                                @RequestParam(required = false) String inactive,
                                @RequestParam(required = false) String logout) {
        model.addAttribute("title", "Login");
        
        if (error != null) {
            model.addAttribute("error", true);
        }
        if (locked != null) {
            model.addAttribute("locked", true);
        }
        if (inactive != null) {
            model.addAttribute("inactive", true);
        }
        if (logout != null) {
            model.addAttribute("logout", true);
        }
        
        return "auth/login";
    }
    
    @GetMapping("/")
    public String redirectRoot(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        return "redirect:/login";
    }
    
    @PostMapping("/logout")
    public String logout() {
        return "redirect:/login?logout=true";
    }
}