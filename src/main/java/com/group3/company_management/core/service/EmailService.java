package com.group3.company_management.core.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendAccountInfo(String to, String username, String rawPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your employee account has been created");
        message.setText("""
                Your employee account has been created.

                Username: %s
                Temporary password: %s

                Please login and change your password.
                """.formatted(username, rawPassword));

        mailSender.send(message);
    }

    public void sendCustomerAccountEmail(String to, String username, String rawPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Your customer account has been created");
        message.setText("""
                Your customer account has been created.

                Login URL: /customer/login
                Username: %s
                Temporary password: %s

                Please login and change your password after signing in.
                """.formatted(username, rawPassword));

        mailSender.send(message);
    }

    public void sendCustomEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    public void sendOtpEmail(String to, String otpCode, int expiryMinutes) {
        System.out.println("\n==================================================================");
        System.out.println("  [GMAIL OTP SYSTEM] MÃ OTP CỦA BẠN LÀ: " + otpCode + " (HẠN 5 PHÚT)");
        System.out.println("  [EMAIL NGƯỜI NHẬN]: " + to);
        System.out.println("==================================================================\n");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("[OTIO System] Mã xác nhận OTP khôi phục mật khẩu");
            message.setText("""
                    Xin chào,

                    Mã OTP để khôi phục mật khẩu của bạn là: %s

                    Mã OTP này có hiệu lực trong vòng %d phút kể từ khi gửi.
                    Vì lý do bảo mật, vui lòng không chia sẻ mã này cho người khác.

                    Trân trọng,
                    Hệ thống Quản lý Doanh nghiệp OTIO
                    """.formatted(otpCode, expiryMinutes));

            mailSender.send(message);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(EmailService.class).warn("Failed to send email via SMTP. Generated OTP code: {}", otpCode);
        }
    }
}

