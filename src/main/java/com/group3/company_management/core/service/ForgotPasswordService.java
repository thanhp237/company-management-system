package com.group3.company_management.core.service;

import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.PasswordResetOtp;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.CustomerRepository;
import com.group3.company_management.core.repository.PasswordResetOtpRepository;
import com.group3.company_management.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int OTP_EXPIRY_MINUTES = 5;

    @Transactional
    public void generateAndSendOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Vui lòng nhập địa chỉ Email");
        }

        String normalizedEmail = email.trim().toLowerCase();

        boolean userExists = userRepository.existsByEmailAndIsDeletedFalse(normalizedEmail);
        boolean customerExists = customerRepository.findByEmailAndNotDeleted(normalizedEmail).isPresent();

        if (!userExists && !customerExists) {
            throw new RuntimeException("Không tìm thấy tài khoản nào đăng ký với Email này trong hệ thống");
        }

        // Generate 6-digit random OTP
        SecureRandom random = new SecureRandom();
        String otpCode = String.format("%06d", random.nextInt(1000000));

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        PasswordResetOtp otpEntity = PasswordResetOtp.builder()
                .email(normalizedEmail)
                .otpCode(otpCode)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        otpRepository.save(otpEntity);

        // Send Email via Gmail SMTP
        emailService.sendOtpEmail(normalizedEmail, otpCode, OTP_EXPIRY_MINUTES);
        log.info("OTP generated successfully for email: {}", normalizedEmail);
    }

    @Transactional
    public String verifyOtp(String email, String otpCode) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Thiếu thông tin Email");
        }
        if (otpCode == null || otpCode.isBlank()) {
            throw new RuntimeException("Vui lòng nhập mã OTP 6 chữ số");
        }

        String normalizedEmail = email.trim().toLowerCase();
        String trimmedOtp = otpCode.trim();

        PasswordResetOtp otp = otpRepository.findValidOtp(normalizedEmail, trimmedOtp, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Mã OTP không chính xác hoặc đã hết hạn (chỉ có hiệu lực trong 5 phút). Vui lòng yêu cầu gửi lại mã!"));

        // Generate one-time reset token
        String resetToken = UUID.randomUUID().toString();
        otp.setResetToken(resetToken);
        otpRepository.save(otp);

        return resetToken;
    }

    @Transactional
    public void resetPassword(String email, String resetToken, String newPassword, String confirmPassword) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Thiếu thông tin Email");
        }
        if (resetToken == null || resetToken.isBlank()) {
            throw new RuntimeException("Mã xác thực reset không hợp lệ");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new RuntimeException("Vui lòng nhập mật khẩu mới");
        }
        if (confirmPassword == null || confirmPassword.isBlank()) {
            throw new RuntimeException("Vui lòng xác nhận mật khẩu mới");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Mật khẩu xác nhận không trùng khớp");
        }

        String normalizedEmail = email.trim().toLowerCase();

        PasswordResetOtp otp = otpRepository.findByEmailAndResetTokenAndUsedFalse(normalizedEmail, resetToken)
                .orElseThrow(() -> new RuntimeException("Yêu cầu đặt lại mật khẩu không hợp lệ hoặc đã quá hạn"));

        String encodedPassword = passwordEncoder.encode(newPassword);

        // Check and update User
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user != null && !user.isDeleted()) {
            user.setPasswordHash(encodedPassword);
            user.setFirstLogin(false);
            userRepository.save(user);
        }

        // Check and update Customer
        Customer customer = customerRepository.findByEmailAndNotDeleted(normalizedEmail).orElse(null);
        if (customer != null) {
            customer.setPasswordHash(encodedPassword);
            customerRepository.save(customer);
        }

        // Mark OTP token as used
        otp.setUsed(true);
        otpRepository.save(otp);

        log.info("Password reset successfully for email: {}", normalizedEmail);
    }
}
