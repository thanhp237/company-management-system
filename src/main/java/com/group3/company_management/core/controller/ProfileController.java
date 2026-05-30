package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.ProfileUpdateRequest;
import com.group3.company_management.core.dto.UserResponse;
import com.group3.company_management.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String viewProfile(Principal principal, Model model) {
        String username = principal.getName();
        UserResponse user = userService.getProfileByUsername(username);
        model.addAttribute("user", user);
        return "profile/index";
    }

    @PostMapping("/update")
    public String updateProfile(Principal principal, @ModelAttribute ProfileUpdateRequest request) {
        String username = principal.getName();
        userService.updateProfile(username, request);
        return "redirect:/profile?success";
    }
}