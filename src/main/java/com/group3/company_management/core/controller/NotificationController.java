package com.group3.company_management.core.controller;

import com.group3.company_management.core.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public String showNotificationPage(Principal principal, Model model) {
        String username = principal.getName();
        model.addAttribute("notifications", notificationService.getNotificationsByUsername(username));
        return "notifications/list";
    }
}