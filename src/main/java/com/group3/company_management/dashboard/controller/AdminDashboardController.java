package com.group3.company_management.dashboard.controller;

import com.group3.company_management.dashboard.service.AdminDashboardService;
import com.group3.company_management.core.dto.SystemMetricsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin dashboard controller
 * Displays system metrics and health monitoring
 */
@Controller
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {
    
    private final AdminDashboardService adminDashboardService;
    
    /**
     * GET /admin/dashboard
     * Display admin dashboard page
     */
    @GetMapping
    public String showAdminDashboard(Model model) {
        SystemMetricsDto metrics = adminDashboardService.getSystemMetrics();
        
        model.addAttribute("title", "Admin Dashboard");
        model.addAttribute("metrics", metrics);
        model.addAttribute("systemStatus", metrics.getSystemStatus());
        model.addAttribute("uptime", metrics.getUptime());
        
        return "admin/dashboard";
    }
    
    /**
     * GET /admin/dashboard/api/metrics
     * Return metrics as JSON for AJAX/charts
     */
    @GetMapping("/api/metrics")
    @ResponseBody
    public SystemMetricsDto getMetricsJson() {
        return adminDashboardService.getSystemMetrics();
    }
}