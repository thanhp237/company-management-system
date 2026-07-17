package com.group3.company_management.core.controller;

// src/main/java/com/group3/company_management/core/controller/CustomerController.java


import com.group3.company_management.core.dto.CustomerRequest;
import com.group3.company_management.core.dto.CustomerResponse;
import com.group3.company_management.core.entity.CustomerActivity;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Opportunity;
import com.group3.company_management.core.repository.OpportunityRepository;
import com.group3.company_management.core.repository.QuotationRepository;
import com.group3.company_management.core.service.CustomerActivityService;
import com.group3.company_management.core.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Customer controller - EXACT SAME PATTERN AS UserController
 * Spring Security 6+ @PreAuthorize for role-based access
 */
@Controller
@RequestMapping("/customers")
@Slf4j
@PreAuthorize("hasAnyRole('SALES', 'MANAGER', 'SALES_MANAGER', 'ADMIN')")
public class CustomerController {
    private final QuotationRepository quotationRepository;
    private final CustomerService customerService;
    private final CustomerActivityService activityService;
    private final OpportunityRepository opportunityRepository;

    @Autowired
    public CustomerController(CustomerService customerService,QuotationRepository quotationRepository, CustomerActivityService activityService, OpportunityRepository opportunityRepository) {
        this.customerService = customerService;
        this.activityService = activityService;
        this.opportunityRepository = opportunityRepository;
        this.quotationRepository=quotationRepository;
    }

    /**
     * GET /customers
     * List all customers with optional status filter
     * Same pattern as UserController.listUsers()
     */
    @GetMapping
    public String listCustomers(@RequestParam(required = false) String status, Model model) {
        log.info("Listing customers with status filter: {}", status);
        
        // Get customers based on status filter
        List<CustomerResponse> customers;
        
        if (status != null && !status.trim().isEmpty()) {
            customers = customerService.getCustomersByStatus(status);
        } else {
            customers = customerService.getAllCustomers();
        }
        
        model.addAttribute("customers", customers);
        return "customers/list";
    }

    /**
     * GET /customers/add
     * Show add/edit form
     * Same pattern as UserController.showAddForm()
     */
    @GetMapping("/add")
    public String showAddForm(@RequestParam(required = false) Long id, Model model) {
        log.info("Showing add/edit form for customer ID: {}", id);
        
        model.addAttribute("customerForm", id == null ? new CustomerRequest() : toRequest(customerService.getCustomerById(id)));
        model.addAttribute("isEdit", id != null);
        return "customers/add-form";
    }

    @GetMapping("/{id}")
    public String detailCustomer(@PathVariable Long id, Model model) {
        log.info("Showing detail for customer ID: {}", id);

        CustomerResponse customer = customerService.getCustomerById(id);
        List<CustomerActivity> activities = activityService.getActivitiesByCustomerId(id);
        Opportunity latestOpportunity = opportunityRepository.findByCustomerIdOrderByUpdatedAtDesc(id)
                .stream()
                .findFirst()
                .orElse(null);
        model.addAttribute("customer", customer);
        model.addAttribute("existsQuotation", quotationRepository.existsByCustomerId(id));
        model.addAttribute("activities", activities);
        model.addAttribute("activityCount", activities.size());
        model.addAttribute("latestOpportunity", latestOpportunity);
        model.addAttribute("canAddInteraction", latestOpportunity == null || !isClosedStage(latestOpportunity.getStage()));
        return "customers/detail";
    }

    /**
     * GET /customers/delete/{id}
     * Delete customer (soft delete)
     * Same pattern as UserController.deleteUser()
     */
    @GetMapping("/delete/{id}")
    public String deleteCustomer(@PathVariable Long id) {
        log.info("Deleting customer with ID: {}", id);
        
        try {
            customerService.deleteCustomer(id);
            return "redirect:/customers";
        } catch (IllegalArgumentException e) {
            log.error("Error deleting customer: {}", e.getMessage());
            return "redirect:/customers";
        }
    }

    /**
     * POST /customers/save
     * Create new customer
     * Same pattern as UserController.saveUser()
     */
    @PostMapping("/save")
    public String saveCustomer(@ModelAttribute("customerForm") CustomerRequest request, Model model) {
        log.info("Saving new customer: {}", request.getFullName());
        
        try {
            customerService.createCustomer(request);
            return "redirect:/customers";
        } catch (IllegalArgumentException exception) {
            log.error("Error saving customer: {}", exception.getMessage());
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("isEdit", false);
            return "customers/add-form";
        }
    }

    /**
     * POST /customers/update
     * Update existing customer
     * Same pattern as UserController.updateUser()
     */
    @PostMapping("/update")
    public String updateCustomer(@ModelAttribute("customerForm") CustomerRequest request, Model model) {
        log.info("Updating customer with ID: {}", request.getId());
        
        try {
            customerService.updateCustomer(request);
            return "redirect:/customers";
        } catch (IllegalArgumentException exception) {
            log.error("Error updating customer: {}", exception.getMessage());
            model.addAttribute("errorMessage", exception.getMessage());
            model.addAttribute("isEdit", true);
            return "customers/add-form";
        }
    }

    /**
     * POST /customers/update-status
     * Update customer status (ACTIVE/INACTIVE)
     * Same pattern as UserController.updateStatus()
     */
    @PostMapping("/update-status")
    public String updateCustomerStatus(@ModelAttribute CustomerRequest request) {
        log.info("Updating customer status - ID: {}, Status: {}", request.getId(), request.getCustomerStatus());
        
        try {
            customerService.updateCustomerStatus(request);
            return "redirect:/customers";
        } catch (IllegalArgumentException e) {
            log.error("Error updating customer status: {}", e.getMessage());
            return "redirect:/customers";
        }
    }

    /**
     * Helper method to convert CustomerResponse to CustomerRequest
     * Same pattern as UserController.toRequest()
     */
    private CustomerRequest toRequest(CustomerResponse response) {
        CustomerRequest request = new CustomerRequest();
        request.setId(response.getId());
        request.setFullName(response.getFullName());
        request.setPhone(response.getPhone());
        request.setEmail(response.getEmail());
        request.setAddress(response.getAddress());
        request.setCustomerSource(response.getCustomerSource());
        request.setAssignedSalesId(response.getAssignedSalesId());
        request.setCustomerStatus(response.getCustomerStatus());
        return request;
    }

    private boolean isClosedStage(String stage) {
        if (stage == null) {
            return false;
        }
        String normalized = stage.trim().toUpperCase();
        return "WON".equals(normalized) || "LOST".equals(normalized);
    }

}
