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
}
