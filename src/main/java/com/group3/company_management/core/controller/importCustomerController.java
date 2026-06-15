package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.LeadDTO;
import com.group3.company_management.core.service.CustomerImportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@Controller
@RequestMapping("/leads")
public class importCustomerController {

    private final CustomerImportService customerImportService;

    public importCustomerController(CustomerImportService customerImportService) {
        this.customerImportService = customerImportService;
    }

    @GetMapping("/import")
    public String showImportPage() {
        return "lead/import";
    }

    @PostMapping("/import")
    public String importCustomer(
            @RequestParam("file") MultipartFile file,
            Model model) {

        try {
            List<LeadDTO> leads = customerImportService.importCustomer(file);
            model.addAttribute("leads", leads);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "lead/import";
    }
}
