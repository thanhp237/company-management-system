package com.group3.company_management.core.security;

import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
@RequiredArgsConstructor
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));


        if (Boolean.TRUE.equals(user.getFirstLogin())) {
            response.sendRedirect("/first-change-password");
            return;
        }

        response.sendRedirect(resolveDashboardUrl(authentication.getAuthorities()));
    }

    private String resolveDashboardUrl(Collection<? extends GrantedAuthority> authorities) {
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();

            return switch (role) {
                case "ROLE_ADMIN" -> "/dashboard/admin";
                case "ROLE_SALES" -> "/dashboard/sales";
                case "ROLE_MARKETING" -> "/dashboard/marketing";
                case "ROLE_ACCOUNTANT" -> "/dashboard/accountant";
                case "ROLE_ADMIN_OFFICER", "ROLE_ADMINOFFICER" -> "/dashboard/admin-officer";
                case "ROLE_SALES_MANAGER", "ROLE_MANAGER" -> "/dashboard/sales-manager";
                case "ROLE_DIRECTOR" -> "/dashboard/director";
                case "ROLE_CUSTOMER" -> "/dashboard/customer";
                default -> "/dashboard";
            };
        }

        return "/dashboard";
    }
}
