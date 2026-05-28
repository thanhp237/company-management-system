package com.group3.company_management.core.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Dashboard controller for authenticated users
 */
@Controller
@RequestMapping("/dashboard")
@PreAuthorize("isAuthenticated()")
public class DashboardController {
    
    /**
     * GET /dashboard
     * Display main dashboard
     */
    @GetMapping
    public String showDashboard(Model model, Authentication authentication) {
        model.addAttribute("title", "Dashboard");
        model.addAttribute("userName", authentication.getName());
        
        // TODO: Replace with actual data from services
        model.addAttribute("totalCustomers", 1234);
        model.addAttribute("activeContracts", 245);
        model.addAttribute("pendingApprovals", 18);
        model.addAttribute("monthlyRevenue", "250000");
        
        return "dashboard/index";
    }
    
    /**
     * GET /dashboard/profile
     * Display user profile
     */
    @GetMapping("/profile")
    public String showProfile(Model model) {
        model.addAttribute("title", "Profile");
        return "dashboard/profile";
    }
    
    /**
     * GET /dashboard/settings
     * Display user settings
     */
    @GetMapping("/settings")
    public String showSettings(Model model) {
        model.addAttribute("title", "Settings");
        return "dashboard/settings";
    }
}