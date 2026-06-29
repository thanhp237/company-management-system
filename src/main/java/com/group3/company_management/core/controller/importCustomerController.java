package com.group3.company_management.core.controller;


import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.service.CustomerImportService;
import com.group3.company_management.core.service.CustomerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
@Controller
@RequestMapping("/customer")
public class importCustomerController {
    private final CustomerImportService customerImportService;
    private CustomerService customerService;

    public importCustomerController(CustomerImportService customerImportService, CustomerService customerService) {
        this.customerImportService = customerImportService;
        this.customerService = customerService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MARKETING','SALES_MANAGER','MANAGER','ADMIN')")
    public String showImportPage(Model model) {
        List<Customer> listCustomer = customerImportService.allCustomer();
        model.addAttribute("customer", listCustomer);
        model.addAttribute("sales", customerImportService.findSale("Sales Staff"));
        return "lead/import";
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('MARKETING','SALES_MANAGER','MANAGER','ADMIN')")
    public String importCustomer(
            @RequestParam("file") MultipartFile file,
            Model model,
            Authentication authentication) {

        String name =authentication.getName();
        try {
            customerImportService.importCustomer(file,name);

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("customer", customerImportService.allCustomer());
            model.addAttribute("sales", customerImportService.findSale("Sales Staff"));
            return "lead/import";
        }

        model.addAttribute("customer", customerImportService.allCustomer());
        model.addAttribute("sales", customerImportService.findSale("Sales Staff"));

        return "lead/import";
    }
    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('SALES_MANAGER','MANAGER','ADMIN')")
    public String checkBox(@RequestParam(value = "checkbox", required = false) List<Long> id, @RequestParam("saleId") Long idSale, Model model) {
       customerImportService.assignCustomersToSale(id, idSale);
       String name = customerImportService.findUser(idSale).getUsername();
        List<Customer> listCustomer = customerImportService.allCustomer();
        model.addAttribute(("nameSale"), name);
        model.addAttribute("customer", listCustomer);
        model.addAttribute("sales", customerImportService.findSale("Sales Staff"));
        return "lead/import";
    }
    @GetMapping("/detail")
    @PreAuthorize("hasAnyRole('MARKETING','SALES_MANAGER','MANAGER','ADMIN')")
    public String detailCustomer(@RequestParam Long id,Model model){
        Customer customer = customerService.findCustomerById(id);
        model.addAttribute("customer",customer);
        return "lead/detail";

    }

    @PostMapping("/detail")
    @PreAuthorize("hasAnyRole('MARKETING','SALES_MANAGER','MANAGER','ADMIN')")
    public String saveImformationCustomer(
            @ModelAttribute("customer") Customer customer,
            Model model) {

        Customer oldCustomer =
                customerService.findCustomerById(customer.getId());

        oldCustomer.setName(customer.getName());
        oldCustomer.setFullName(customer.getName());
        oldCustomer.setEmail(customer.getEmail());
        oldCustomer.setAddress(customer.getAddress());
        oldCustomer.setPhone(customer.getPhone());
        oldCustomer.setGender(customer.getGender());
        oldCustomer.setCustomerSource(customer.getCustomerSource());
        oldCustomer.setCustomerStatus(customer.getCustomerStatus());
        oldCustomer.setOpportunityLevel(customer.getOpportunityLevel());

        customerService.saveCustomer(oldCustomer);

        return "redirect:/customer/detail?id=" + oldCustomer.getId();
    }

}
