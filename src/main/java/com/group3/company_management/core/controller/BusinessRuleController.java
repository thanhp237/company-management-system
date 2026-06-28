package com.group3.company_management.core.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/business-rules")
@PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER')")
public class BusinessRuleController {

    @GetMapping
    public String showRules(Model model) {
        model.addAttribute("leadAssignmentMode", "ROUND_ROBIN");
        model.addAttribute("commissionRate", 5);
        model.addAttribute("voucherMinValue", 5000000);
        model.addAttribute("voucherDuration", 30);
        model.addAttribute("approvalRules", 2);
        return "admin/business-rules";
    }

    @PostMapping("/save")
    public String saveRules(
            @RequestParam(required = false) String leadAssignmentMode,
            @RequestParam(required = false) Integer commissionRate,
            @RequestParam(required = false) Integer voucherMinValue,
            @RequestParam(required = false) Integer voucherDuration,
            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu cấu hình quy tắc nghiệp vụ.");
        return "redirect:/business-rules";
    }
}
