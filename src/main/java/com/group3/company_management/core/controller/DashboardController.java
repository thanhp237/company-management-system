package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    /**
     * GET /dashboard
     * Display main dashboard
     */
    // @GetMapping
    // public String showDashboard(Model model, Authentication authentication) {
    //     model.addAttribute("title", "Dashboard");
    //     model.addAttribute("userName", authentication == null ? "Developer" : authentication.getName());

    //     // TODO: Replace with actual data from services
    //     model.addAttribute("totalCustomers", 0);
    //     model.addAttribute("activeContracts", 0);
    //     model.addAttribute("pendingApprovals", 0);
    //     model.addAttribute("monthlyRevenue", "0");

    //     return "dashboard/index";
    // }

    @GetMapping
    public String redirectDashboard(Authentication auth) {

        String role = auth.getAuthorities()
                .stream()
                .findFirst()
                .get()
                .getAuthority();

        switch (role) {

            case "ROLE_ADMIN":
                return "redirect:/dashboard/admin";

            case "ROLE_SALES":
                return "redirect:/dashboard/sales";

            case "ROLE_MARKETING":
                return "redirect:/dashboard/marketing";

            case "ROLE_ACCOUNTANT":
                return "redirect:/dashboard/accountant";

            case "ROLE_ADMIN_OFFICER":
            case "ROLE_ADMINOFFICER":
                return "redirect:/dashboard/admin-officer";

            case "ROLE_SALES_MANAGER":
                return "redirect:/dashboard/sales-manager";

            case "ROLE_DIRECTOR":
                return "redirect:/dashboard/director";

            default:
                return "redirect:/access-denied";
        }
    }

        /**
     * GET /dashboard
     * Display sales dashboard
     */
    @GetMapping("/sales")
    @PreAuthorize("hasRole('SALES')")
    public String salesDashboard(
            Model model,
            Authentication authentication) {

        model.addAttribute("title", "Sales Dashboard");
        model.addAttribute("userName", authentication.getName());

        return "dashboard/sales";
    }

        @GetMapping("/marketing")
    @PreAuthorize("hasRole('MARKETING')")
    public String marketingDashboard(
            Model model,
            Authentication authentication) {

        model.addAttribute("title", "Marketing Dashboard");
        model.addAttribute("userName", authentication.getName());

        return "dashboard/marketing";
    }
    @GetMapping("/accountant")
    @PreAuthorize("hasRole('ACCOUNTANT')")
    public String accountantDashboard(
            Model model,
            Authentication authentication) {

        model.addAttribute("title", "Accountant Dashboard");
        model.addAttribute("userName", authentication.getName());

        return "dashboard/accountant";
    }

    @GetMapping("/admin-officer")
    @PreAuthorize("hasRole('ADMIN_OFFICER')")
    public String adminOfficerDashboard(
            Model model,
            Authentication authentication) {

        model.addAttribute("title", "Admin Officer Dashboard");
        model.addAttribute("userName", authentication.getName());

        return "dashboard/admin-officer";
    }

    @GetMapping("/sales-manager")
    @PreAuthorize("hasRole('SALES_MANAGER')")
    public String salesManagerDashboard(
            Model model,
            Authentication authentication) {

        model.addAttribute("title", "Sales Manager Dashboard");
        model.addAttribute("userName", authentication.getName());

        return "dashboard/sales-manager";
    }
    @GetMapping("/director")
    @PreAuthorize("hasRole('DIRECTOR')")
    public String directorDashboard(
            Model model,
            Authentication authentication) {

        model.addAttribute("title", "Director Dashboard");
        model.addAttribute("userName", authentication.getName());

        return "dashboard/director";
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

        return "dashboard/index";
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
