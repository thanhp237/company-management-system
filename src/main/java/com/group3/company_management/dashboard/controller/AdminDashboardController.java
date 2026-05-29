package com.group3.company_management.dashboard.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.group3.company_management.core.dto.SystemMetricsDto;
import com.group3.company_management.dashboard.service.AdminDashboardService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {
    
    private final AdminDashboardService adminDashboardService;
    
    @GetMapping
    public String showDashboard(Model model, Authentication authentication) {
        model.addAttribute("title", "Dashboard");
        model.addAttribute("userName", authentication.getName());
        model.addAttribute("totalCustomers", 1234);
        model.addAttribute("activeContracts", 245);
        model.addAttribute("pendingApprovals", 18);
        model.addAttribute("monthlyRevenue", "250000");
        
        log.info("User dashboard accessed: {}", authentication.getName());
        return "dashboard/index";
    }
    
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String showAdminDashboard(Model model, Authentication authentication) {
        log.info("Admin dashboard accessed: {}", authentication.getName());
        
        SystemMetricsDto metrics = adminDashboardService.getSystemMetrics();
        
        model.addAttribute("title", "Admin Dashboard");
        model.addAttribute("metrics", metrics);
        model.addAttribute("systemStatus", metrics.getSystemStatus());
        model.addAttribute("uptime", metrics.getUptime());
        model.addAttribute("userName", authentication.getName());
        
        return "admin/dashboard";
    }
    
    @GetMapping("/admin/api/metrics")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public SystemMetricsDto getMetricsJson() {
        return adminDashboardService.getSystemMetrics();
    }
}