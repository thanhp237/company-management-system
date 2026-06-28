package com.group3.company_management.core.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "auth/select-login";
    }

    @GetMapping("/select-login")
    public String selectLogin() {
        return "auth/select-login";
    }
}