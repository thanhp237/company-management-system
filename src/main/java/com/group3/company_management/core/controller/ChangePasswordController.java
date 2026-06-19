package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.group3.company_management.core.dto.ChangePasswordDTO;
import com.group3.company_management.core.service.ChangePasswordService;

import lombok.RequiredArgsConstructor;

@Controller


public class ChangePasswordController {

    private final ChangePasswordService changePasswordService;
    private final UserRepository userRepository;

    public ChangePasswordController(ChangePasswordService changePasswordService, UserRepository userRepository) {
        this.changePasswordService = changePasswordService;
        this.userRepository = userRepository;
    }

    @GetMapping("/login-success")
    public String loginSuccess(Authentication authentication) {

        User user = userRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (Boolean.TRUE.equals(user.getFirstLogin())) {
            return "redirect:/first-change-password";
        }

        return "redirect:/dashboard";
    }
    @GetMapping("/first-change-password")
    public String showFirstChangePasswordForm(Model model) {
        model.addAttribute("changePassword", new ChangePasswordDTO());
        return "change-password/first-change";
    }

    @PostMapping("/first-change-password")
    public String firstChangePassword(
            @ModelAttribute("changePassword") ChangePasswordDTO dto,
            Authentication authentication,
            Model model) {

        try {
            String username = authentication.getName();

            changePasswordService.changeFirstPassword(username, dto);

            return "redirect:/dashboard";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "change-password/first-change";
        }
    }


    @GetMapping("/change-password")

    public String showChangePasswordForm(Model model) {
        model.addAttribute("changePassword", new ChangePasswordDTO());
        return "change-password/change";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @ModelAttribute("changePassword") ChangePasswordDTO dto,
            Authentication authentication,
            Model model) {

        try {
            String username = authentication.getName();

            changePasswordService.changePasswordInSettings(username, dto);

            return "redirect:/dashboard/settings";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "change-password/change";
        }
    }
}