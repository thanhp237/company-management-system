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



    // @GetMapping
    // public String redirectDashboard(Authentication auth) {

    //     String role = auth.getAuthorities()
    //             .stream()
    //             .findFirst()
    //             .get()
    //             .getAuthority();

    //     switch (role) {

    //         case "ROLE_ADMIN":
    //             return "redirect:/dashboard/admin";

    //         case "ROLE_SALES":
    //             return "redirect:/dashboard/sales";

    //         case "ROLE_MARKETING":
    //             return "redirect:/dashboard/marketing";

    //         case "ROLE_ACCOUNTANT":
    //             return "redirect:/dashboard/accountant";

    //         case "ROLE_ADMIN_OFFICER":
    //         case "ROLE_ADMINOFFICER":
    //             return "redirect:/dashboard/admin-officer";

    //         case "ROLE_SALES_MANAGER":
    //             return "redirect:/dashboard/sales-manager";

    //         case "ROLE_DIRECTOR":
    //             return "redirect:/dashboard/director";

    //         default:
    //             return "redirect:/access-denied";
    //     }
    // // }

    @GetMapping
    public String redirectDashboard(Authentication auth) {

        String role = auth.getAuthorities()
                .stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("");

        if (role.equals("ROLE_CUSTOMER")) {
            return "redirect:/dashboard/customer";
        }

        return "redirect:/dashboard/employee";
    }

    /**

     * Gộp tất cả role 

     */
@GetMapping("/employee")
@PreAuthorize("""
    hasAnyRole(
        'ADMIN',
        'ADMIN_OFFICER',
        'SALES',
        'MARKETING',
        'SALES_MANAGER',
        'ACCOUNTANT',
        'DIRECTOR'
    )
""")
public String employeeDashboard(Authentication authentication,
                                Model model) {

    String role = authentication.getAuthorities()
            .stream()
            .findFirst()
            .map(a -> a.getAuthority().replace("ROLE_", ""))
            .orElse("");

    model.addAttribute("title", "Employee Dashboard");
    model.addAttribute("userName", authentication.getName());
    model.addAttribute("role", role);

    // ===== Common Dashboard Data =====
    model.addAttribute("totalCustomers", 0);
    model.addAttribute("activeContracts", 0);
    model.addAttribute("pendingApprovals", 0);
    model.addAttribute("monthlyRevenue", 0);

    return "dashboard/employee";
}

    /**
     * =========================
     * CUSTOMER DASHBOARD
     * =========================
     */
    @GetMapping("/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public String customerDashboard(Authentication authentication, Model model) {

        model.addAttribute("title", "Customer Dashboard");
        model.addAttribute("userName", authentication.getName());

        // TODO: customer data
        model.addAttribute("orderCount", 0);

        return "dashboard/customer";
    }
}
