package com.group3.company_management.core.controller;

import com.group3.company_management.core.service.ForgotPasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordPage(Model model) {
        model.addAttribute("title", "Quên Mật Khẩu - OTIO");
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            forgotPasswordService.generateAndSendOtp(email);
            redirectAttributes.addFlashAttribute("successMessage", "Mã OTP 6 chữ số đã được gửi tới Gmail của bạn. Mã có hiệu lực trong 5 phút.");
            return "redirect:/verify-otp?email=" + email.trim();
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("title", "Quên Mật Khẩu - OTIO");
            return "auth/forgot-password";
        }
    }

    @GetMapping("/verify-otp")
    public String showVerifyOtpPage(@RequestParam("email") String email, Model model) {
        model.addAttribute("title", "Xác Thực Mã OTP - OTIO");
        model.addAttribute("email", email);
        return "auth/verify-otp";
    }

    @PostMapping("/verify-otp")
    public String processVerifyOtp(
            @RequestParam("email") String email,
            @RequestParam("otpCode") String otpCode,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            String resetToken = forgotPasswordService.verifyOtp(email, otpCode);
            return "redirect:/reset-password?email=" + email.trim() + "&token=" + resetToken;
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("otpCode", otpCode);
            model.addAttribute("title", "Xác Thực Mã OTP - OTIO");
            return "auth/verify-otp";
        }
    }

    @GetMapping("/reset-password")
    public String showResetPasswordPage(
            @RequestParam("email") String email,
            @RequestParam("token") String token,
            Model model) {

        model.addAttribute("title", "Đặt Lại Mật Khẩu Mới - OTIO");
        model.addAttribute("email", email);
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam("email") String email,
            @RequestParam("token") String token,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            forgotPasswordService.resetPassword(email, token, newPassword, confirmPassword);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập bằng mật khẩu mới.");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("email", email);
            model.addAttribute("token", token);
            model.addAttribute("title", "Đặt Lại Mật Khẩu Mới - OTIO");
            return "auth/reset-password";
        }
    }
}
