package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.ChangePasswordDTO;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChangePasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changeFirstPassword(String username, ChangePasswordDTO dto) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if (!Boolean.TRUE.equals(user.getFirstLogin())) {
            throw new RuntimeException("Tài khoản này không ở trạng thái đăng nhập lần đầu");
        }

        if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
            throw new RuntimeException("Vui lòng nhập mật khẩu mới");
        }

        if (dto.getConfirmPassword() == null || dto.getConfirmPassword().isBlank()) {
            throw new RuntimeException("Vui lòng xác nhận mật khẩu mới");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));

        user.setFirstLogin(false);

        userRepository.save(user);
    }

    @Transactional
    public void changePasswordInSettings(String username, ChangePasswordDTO dto) {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));

        if (dto.getCurrentPassword() == null || dto.getCurrentPassword().isBlank()) {
            throw new RuntimeException("Vui lòng nhập mật khẩu hiện tại");
        }

        if (dto.getNewPassword() == null || dto.getNewPassword().isBlank()) {
            throw new RuntimeException("Vui lòng nhập mật khẩu mới");
        }

        if (dto.getConfirmPassword() == null || dto.getConfirmPassword().isBlank()) {
            throw new RuntimeException("Vui lòng xác nhận mật khẩu mới");
        }

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Mật khẩu xác nhận không khớp");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));

        userRepository.save(user);
    }
}
