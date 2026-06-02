package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Dashboard controller for authenticated users
 */
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    /**
     * GET /dashboard
     * Display main dashboard
     */
    @GetMapping
    public String showDashboard(Model model, Authentication authentication) {
        model.addAttribute("title", "Dashboard");
        model.addAttribute("userName", authentication == null ? "Developer" : authentication.getName());

        // TODO: Replace with actual data from services
        model.addAttribute("totalCustomers", 0);
        model.addAttribute("activeContracts", 0);
        model.addAttribute("pendingApprovals", 0);
        model.addAttribute("monthlyRevenue", "0");

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

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAdminDashboard(Model model, Authentication authentication) {
        model.addAttribute("title", "Admin Dashboard");
        model.addAttribute("userName", authentication.getName());

        // User user =
        // userRepository.findByUsername(authentication.getName()).orElse(null);
        // if (user != null) {
        // log.info("Admin dashboard accessed by admin: {}", user.getUsername());
        // }

        // Admin-specific metrics
        model.addAttribute("systemStatus", "HEALTHY");
        model.addAttribute("activeUsers", 24);
        model.addAttribute("failedLogins", 3);
        model.addAttribute("uptime", "45 days, 12 hours");

        return "admin/dashboard";
    }

    public String showUserManagement(Model model, Authentication authentication) {
        model.addAttribute("title", "User Management");
        model.addAttribute("userName", authentication.getName());

        log.info("User management page accessed by admin: {}", authentication.getName());
        return "admin/users";
    }

    @GetMapping("/admin/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAuditLogs(Model model, Authentication authentication) {
        model.addAttribute("title", "Audit Logs");
        model.addAttribute("userName", authentication.getName());

        log.info("Audit logs page accessed by admin: {}", authentication.getName());
        return "admin/audit-logs";
    }

}
