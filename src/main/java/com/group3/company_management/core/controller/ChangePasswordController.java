package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.ChangePasswordDTO;
import com.group3.company_management.core.service.ChangePasswordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ChangePasswordController {

    private final ChangePasswordService changePasswordService;

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