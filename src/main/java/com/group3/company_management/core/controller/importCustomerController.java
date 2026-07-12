package com.group3.company_management.core.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final CustomerService customerService;

    public importCustomerController(CustomerImportService customerImportService, CustomerService customerService) {
        this.customerImportService = customerImportService;
        this.customerService = customerService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MARKETING','SALES_MANAGER','MANAGER','ADMIN')")
    public String showImportPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {

        Sort sortObj = "oldest".equalsIgnoreCase(sort)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Customer> customerPage = customerImportService.allCustomer(status, pageable);
        addCommonAttributes(model, customerPage, page, size, status, sort);
        return "lead/import";
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('MARKETING','SALES_MANAGER','MANAGER','ADMIN')")
    public String importCustomer(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "newest") String sort,
            Model model,
            Authentication authentication) {

        String name = authentication.getName();
        try {
            customerImportService.importCustomer(file, name);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            Sort sortObj = "oldest".equalsIgnoreCase(sort)
                    ? Sort.by("createdAt").ascending()
                    : Sort.by("createdAt").descending();
            Pageable pageable = PageRequest.of(page, size, sortObj);
            Page<Customer> customerPage = customerImportService.allCustomer(status, pageable);
            addCommonAttributes(model, customerPage, page, size, status, sort);
            return "lead/import";
        }

        // Sau khi import thành công, đưa về trang 0 và hiển thị lead mới nhất
        Sort sortObj = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(0, size, sortObj);
        Page<Customer> customerPage = customerImportService.allCustomer("all", pageable);
        addCommonAttributes(model, customerPage, 0, size, "all", "newest");

        return "lead/import";
    }

    @PostMapping("/check")
    @PreAuthorize("hasAnyRole('SALES_MANAGER','MANAGER','ADMIN')")
    public String checkBox(
            @RequestParam(value = "checkbox", required = false) List<Long> id,
            @RequestParam("saleId") Long idSale,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "newest") String sort,
            Model model) {
        customerImportService.assignCustomersToSale(id, idSale);
        String name = customerImportService.findUser(idSale).getUsername();

        Sort sortObj = "oldest".equalsIgnoreCase(sort)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Customer> customerPage = customerImportService.allCustomer(status, pageable);

        model.addAttribute("nameSale", name);
        addCommonAttributes(model, customerPage, page, size, status, sort);
        return "lead/import";
    }

    @GetMapping("/detail")
    @PreAuthorize("hasAnyRole('MARKETING','SALES_MANAGER','MANAGER','ADMIN')")
    public String detailCustomer(@RequestParam Long id, Model model) {
        Customer customer = customerService.findCustomerById(id);
        model.addAttribute("customer", customer);
        return "lead/detail";
    }

    @PostMapping("/detail")
    @PreAuthorize("hasAnyRole('MARKETING','SALES_MANAGER','MANAGER','ADMIN')")
    public String saveImformationCustomer(
            @ModelAttribute("customer") Customer customer,
            Model model) {

        Customer oldCustomer = customerService.findCustomerById(customer.getId());

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

    // Hàm phụ trợ dùng chung có 6 tham số đầy đủ
    private void addCommonAttributes(Model model, Page<Customer> customerPage, int page, int size, String status, String sort) {
        model.addAttribute("customerPage", customerPage);
        model.addAttribute("customer", customerPage.getContent());
        model.addAttribute("sales", customerImportService.findSale("Sales Staff"));
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentSort", sort);

        model.addAttribute("totalCustomers", customerImportService.countTotalCustomers());
        model.addAttribute("unassignedCustomers", customerImportService.countUnassignedCustomers());
        model.addAttribute("assignedCustomers", customerImportService.countAssignedCustomers());
    }
}