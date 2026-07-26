package com.group3.company_management.core.security;

import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class FirstLoginInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    public FirstLoginInterceptor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        boolean isCustomer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
        if (isCustomer) {
            return true;
        }
        String requestURI = request.getRequestURI();

        if (requestURI.equals("/first-change-password") || 
            requestURI.equals("/logout") || 
            requestURI.startsWith("/css/") || 
            requestURI.startsWith("/js/") || 
            requestURI.startsWith("/images/") || 
            requestURI.startsWith("/uploads/") || 
            requestURI.startsWith("/webjars/") || 
            requestURI.equals("/css/shared/main.css") ||
            requestURI.startsWith("/api/")) {
            return true;
        }


        String username = auth.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (Boolean.TRUE.equals(user.getFirstLogin())) {

                response.sendRedirect("/first-change-password");
                return false;
            }
        }

        return true;
    }
}

