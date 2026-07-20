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
        // 1. Lấy thông tin đăng nhập hiện tại từ Security Context
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Nếu chưa đăng nhập hoặc là tài khoản ẩn danh, cho qua (để Spring Security xử lý chuyển hướng đăng nhập)
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        // Nếu là khách hàng (ROLE_CUSTOMER), cho qua vì khách hàng không có cơ chế bắt buộc đổi mật khẩu này
        boolean isCustomer = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));
        if (isCustomer) {
            return true;
        }

        // 2. Lấy URL mà người dùng đang cố gắng truy cập
        String requestURI = request.getRequestURI();

        // 3. Nếu là các trang đổi mật khẩu, trang đăng xuất, hoặc các tài nguyên giao diện (.css, .js...) thì CHO QUA
        // Tránh lỗi vòng lặp chuyển hướng vô tận (infinite redirect loop)
        if (requestURI.equals("/first-change-password") || 
            requestURI.equals("/logout") || 
            requestURI.startsWith("/css/") || 
            requestURI.startsWith("/js/") || 
            requestURI.startsWith("/images/") || 
            requestURI.startsWith("/uploads/") || 
            requestURI.startsWith("/webjars/") || 
            requestURI.equals("/main.css") ||
            requestURI.startsWith("/api/")) {
            return true;
        }

        // 4. Lấy thông tin tài khoản nhân viên từ database để kiểm tra trạng thái đổi mật khẩu lần đầu
        String username = auth.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Nếu tài khoản này yêu cầu đổi mật khẩu lần đầu (firstLogin = true)
            if (Boolean.TRUE.equals(user.getFirstLogin())) {
                // Bắt buộc chuyển hướng về trang đổi mật khẩu
                response.sendRedirect("/first-change-password");
                return false; // Chặn yêu cầu hiện tại, không cho truy cập URL khác
            }
        }

        return true; // Cho phép truy cập bình thường
    }
}
