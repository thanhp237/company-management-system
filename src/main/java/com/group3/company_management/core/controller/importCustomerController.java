package com.group3.company_management.core.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.service.CustomerImportService;
import com.group3.company_management.core.service.CustomerService;
import com.group3.company_management.core.service.SalesTargetService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.time.YearMonth;

@Controller
@RequestMapping("/customer")
public class importCustomerController {
    private final CustomerImportService customerImportService;
    private final CustomerService customerService;
    private final SalesTargetService salesTargetService;

    public importCustomerController(CustomerImportService customerImportService,
                                    CustomerService customerService,
                                    SalesTargetService salesTargetService) {
        this.customerImportService = customerImportService;
        this.customerService = customerService;
        this.salesTargetService = salesTargetService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MARKETING','SALES_MANAGER','MANAGER','ADMIN')")
    public String showImportPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "newest") String sort,
            Authentication authentication,
            Model model) {

        Sort sortObj = "oldest".equalsIgnoreCase(sort)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();

        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Customer> customerPage = customerImportService.allCustomer(status, pageable);
        addCommonAttributes(model, customerPage, page, size, status, sort, authentication);
        return "lead/import";
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('MARKETING','ADMIN')")
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
            addCommonAttributes(model, customerPage, page, size, status, sort, authentication);
            return "lead/import";
        }


        Sort sortObj = Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(0, size, sortObj);
        Page<Customer> customerPage = customerImportService.allCustomer("all", pageable);
        addCommonAttributes(model, customerPage, 0, size, "all", "newest", authentication);

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
            Authentication authentication,
            Model model) {
        String name = null;
        try {
            customerImportService.assignCustomersToSale(id, idSale, authentication.getName());
            name = customerImportService.findUser(idSale).getUsername();
        } catch (RuntimeException exception) {
            model.addAttribute("error", exception.getMessage());
        }

        Sort sortObj = "oldest".equalsIgnoreCase(sort)
                ? Sort.by("createdAt").ascending()
                : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortObj);
        Page<Customer> customerPage = customerImportService.allCustomer(status, pageable);

        if (name != null) {
            model.addAttribute("nameSale", name);
        }
        addCommonAttributes(model, customerPage, page, size, status, sort, authentication);
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
    @PreAuthorize("hasAnyRole('MARKETING','ADMIN')")
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


    private void addCommonAttributes(Model model, Page<Customer> customerPage, int page, int size, String status, String sort, Authentication authentication) {
        model.addAttribute("customerPage", customerPage);
        model.addAttribute("customer", customerPage.getContent());
        try {
            model.addAttribute("sales", customerImportService.findAssignableSales(authentication.getName()));
        } catch (RuntimeException exception) {
            model.addAttribute("sales", List.of());
            if (!model.containsAttribute("error")) {
                model.addAttribute("error", exception.getMessage());
            }
        }
        model.addAttribute("currentPage", page);
        model.addAttribute("size", size);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentSort", sort);

        model.addAttribute("totalCustomers", customerImportService.countTotalCustomers());
        model.addAttribute("unassignedCustomers", customerImportService.countUnassignedCustomers());
        model.addAttribute("assignedCustomers", customerImportService.countAssignedCustomers());
        if (canViewTargets(authentication)) {
            try {
                model.addAttribute("targetSummaries", salesTargetService.getTargetSummaries(authentication.getName(), YearMonth.now()));
            } catch (RuntimeException exception) {
                if (!model.containsAttribute("error")) {
                    model.addAttribute("error", exception.getMessage());
                }
            }
        }
    }

    private boolean canViewTargets(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null || authentication.getName() == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(authority ->
                "ROLE_SALES_MANAGER".equals(authority.getAuthority())
                        || "ROLE_MANAGER".equals(authority.getAuthority())
                        || "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
