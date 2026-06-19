package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.LeadDTO;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.service.CustomerImportService;
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

    public importCustomerController(CustomerImportService customerImportService) {
        this.customerImportService = customerImportService;
    }

    @GetMapping("/import")
    public String showImportPage(Model model) {

        List<Customer> listCustomer = customerImportService.allCustomer();

        model.addAttribute("customer", listCustomer);
        model.addAttribute("sales", customerImportService.findSale("Sales Staff"));
        return "lead/import";
    }

    @PostMapping("/import")
    public String importCustomer(
            @RequestParam("file") MultipartFile file,
            Model model,
            Authentication authentication) {

        String name =authentication.getName();
        try {
            customerImportService.importCustomer(file,name);

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "lead/import";
    }
    @PostMapping("/check")
    public String checkBox(@RequestParam("checkbox") List<Long> id, @RequestParam("saleId") Long idSale, Model model) {
       for (Long  custommerId: id ){
           Customer customer =  customerImportService.findCustomerById(custommerId);
           customer.setAssignedSalesId(idSale);
           customerImportService.saveCustomer(customer);

       }
       String name =(customerImportService.findUser(idSale)).getUsername();
        List<Customer> listCustomer = customerImportService.allCustomer();
        model.addAttribute(("nameSale"), name);
        model.addAttribute("customer", listCustomer);
        model.addAttribute("sales", customerImportService.findSale("Sales Staff"));
        return "lead/import";
    }

}
