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
                .orElseThrow(() -> new RuntimeException("User not found"));

        // FIRST LOGIN CHECK (giữ nguyên logic cũ)
        if (Boolean.TRUE.equals(user.getFirstLogin())) {
            response.sendRedirect("/first-change-password");
            return;
        }

        // ROLE-BASED REDIRECT
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        String redirectUrl = "/dashboard/employee"; // default

        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();

            if (role.equals("ROLE_CUSTOMER")) {
                redirectUrl = "/dashboard/customer";
                break;
            }
        }

        response.sendRedirect(redirectUrl);
    }
}