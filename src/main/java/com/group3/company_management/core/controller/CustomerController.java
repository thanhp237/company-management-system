package com.group3.company_management.core.controller;

// src/main/java/com/group3/company_management/core/controller/CustomerController.java


import com.group3.company_management.core.dto.CustomerRequest;
import com.group3.company_management.core.dto.CustomerResponse;
import com.group3.company_management.core.entity.CustomerActivity;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.entity.Invoice;
import com.group3.company_management.core.entity.Opportunity;
import com.group3.company_management.core.repository.ContractRepository;
import com.group3.company_management.core.repository.InvoiceRepository;
import com.group3.company_management.core.repository.OpportunityRepository;
import com.group3.company_management.core.repository.QuotationRepository;
import com.group3.company_management.core.service.CustomerActivityService;
import com.group3.company_management.core.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Customer controller - EXACT SAME PATTERN AS UserController
 * Spring Security 6+ @PreAuthorize for role-based access
 */
@Controller
@RequestMapping("/customers")
@Slf4j
@PreAuthorize("hasAnyRole('MARKETING', 'SALES', 'MANAGER', 'SALES_MANAGER', 'ADMIN', 'ADMIN_OFFICER', 'ADMINOFFICER', 'ACCOUNTANT', 'DIRECTOR')")
public class CustomerController {
    private final QuotationRepository quotationRepository;
    private final CustomerService customerService;
    private final CustomerActivityService activityService;
    private final OpportunityRepository opportunityRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;

    @Autowired

    public CustomerController(CustomerService customerService,
                              CustomerActivityService activityService,
                              OpportunityRepository opportunityRepository,
                              ContractRepository contractRepository,
                              QuotationRepository quotationRepository,
                              InvoiceRepository invoiceRepository) {
        this.customerService = customerService;
        this.activityService = activityService;
        this.opportunityRepository = opportunityRepository;
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.quotationRepository= quotationRepository;

    }

    /**
     * GET /customers
     * List all customers with optional status filter
     * Same pattern as UserController.listUsers()
     */
    @GetMapping
    public String listCustomers(@RequestParam(required = false) String status,
                                Authentication authentication,
                                Model model) {
        log.info("Listing customers with status filter: {}", status);
        
        // Get customers based on status filter
        List<CustomerResponse> customers;
        
        if (status != null && !status.trim().isEmpty()) {
            customers = customerService.getCustomersByStatus(status);
        } else {
            customers = customerService.getAllCustomers();
        }
        
        model.addAttribute("customers", customers);
        addPermissionFlags(model, authentication);
        return "customers/list";
    }


    /**
     * GET /customers/add
     * Show add/edit form
     * Same pattern as UserController.showAddForm()
     */
    @GetMapping("/add")
    @PreAuthorize("hasAnyRole('MARKETING', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
    public String showAddForm(@RequestParam(required = false) Long id, Model model) {
        log.info("Showing add/edit form for customer ID: {}", id);
        
        model.addAttribute("customerForm", id == null ? new CustomerRequest() : toRequest(customerService.getCustomerById(id)));
        model.addAttribute("isEdit", id != null);
        return "customers/add-form";
    }

    @GetMapping("/{id}")
    public String detailCustomer(@PathVariable Long id,
                                 Authentication authentication,
                                 Model model) {
        log.info("Showing detail for customer ID: {}", id);

        CustomerResponse customer = customerService.getCustomerById(id);
        List<CustomerActivity> activities = activityService.getActivitiesByCustomerId(id);
        List<Contract> contracts = contractRepository.findByCustomerIdOrderByCreatedAtDesc(id);
        List<Invoice> invoices = invoiceRepository.findByContractCustomerIdOrderByCreatedAtDesc(id);
        Opportunity latestOpportunity = opportunityRepository.findByCustomerIdOrderByUpdatedAtDesc(id)
                .stream()
                .findFirst()
                .orElse(null);
        model.addAttribute("customer", customer);
        model.addAttribute("existsQuotation", quotationRepository.existsByCustomerId(id));
        model.addAttribute("activities", activities);
        model.addAttribute("activityCount", activities.size());
        model.addAttribute("contracts", contracts);
        model.addAttribute("invoices", invoices);
        model.addAttribute("contractCount", contracts.size());
        model.addAttribute("signedContractCount", contracts.stream().filter(contract -> Contract.ContractStatus.SIGNED.equals(contract.getStatus())).count());
        model.addAttribute("invoiceCount", invoices.size());
        model.addAttribute("draftInvoiceCount", invoices.stream().filter(invoice -> Invoice.InvoiceStatus.DRAFT.equals(invoice.getStatus())).count());
        model.addAttribute("issuedInvoiceCount", invoices.stream().filter(invoice -> Invoice.InvoiceStatus.ISSUED.equals(invoice.getStatus())).count());
        model.addAttribute("paidInvoiceCount", invoices.stream().filter(invoice -> Invoice.InvoiceStatus.PAID.equals(invoice.getStatus())).count());
        model.addAttribute("totalInvoiceAmount", sumInvoices(invoices, null));
        model.addAttribute("issuedInvoiceAmount", sumInvoices(invoices, Invoice.InvoiceStatus.ISSUED));
        model.addAttribute("paidInvoiceAmount", invoices.stream()
                .map(invoice -> value(invoice.getPaidAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("outstandingInvoiceAmount", invoices.stream()
                .map(invoice -> value(invoice.getOutstandingAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("latestOpportunity", latestOpportunity);
        addPermissionFlags(model, authentication);
        model.addAttribute("reportTitle", reportTitle(authentication));
        model.addAttribute("reportSubtitle", reportSubtitle(authentication));
        model.addAttribute("canAddInteraction", canUseInteraction(authentication)
                && (latestOpportunity == null || !isClosedStage(latestOpportunity.getStage())));
        return "customers/detail";
    }

    /**
     * GET /customers/delete/{id}
     * Delete customer (soft delete)
     * Same pattern as UserController.deleteUser()
     */
    @GetMapping("/delete/{id}")
    @PreAuthorize("hasAnyRole('MARKETING', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('MARKETING', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('MARKETING', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('MARKETING', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
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

    private void addPermissionFlags(Model model, Authentication authentication) {
        model.addAttribute("canManageCustomer", hasAnyRole(authentication, "MARKETING", "SALES_MANAGER", "MANAGER", "ADMIN"));
        model.addAttribute("canCreateQuotation", hasAnyRole(authentication, "SALES", "SALES_MANAGER", "MANAGER", "ADMIN"));
        model.addAttribute("canUseInteraction", canUseInteraction(authentication));
        model.addAttribute("canViewFinanceReport", hasAnyRole(authentication, "ACCOUNTANT", "ADMIN", "DIRECTOR", "MANAGER", "SALES_MANAGER"));
        model.addAttribute("canViewContractReport", hasAnyRole(authentication, "ADMIN_OFFICER", "ADMINOFFICER", "ADMIN", "ACCOUNTANT", "DIRECTOR", "MANAGER", "SALES_MANAGER", "SALES"));
    }

    private boolean canUseInteraction(Authentication authentication) {
        return hasAnyRole(authentication, "MARKETING", "SALES", "SALES_MANAGER", "MANAGER", "ADMIN");
    }

    private boolean hasAnyRole(Authentication authentication, String... roles) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        for (String role : roles) {
            String authority = "ROLE_" + role;
            boolean matched = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private String reportTitle(Authentication authentication) {
        if (hasAnyRole(authentication, "ACCOUNTANT")) {
            return "Báo cáo tài chính khách hàng";
        }
        if (hasAnyRole(authentication, "ADMIN_OFFICER", "ADMINOFFICER")) {
            return "Báo cáo hợp đồng và điều khoản";
        }
        if (hasAnyRole(authentication, "DIRECTOR")) {
            return "Báo cáo tổng hợp khách hàng";
        }
        if (hasAnyRole(authentication, "MARKETING")) {
            return "Báo cáo nguồn và chất lượng khách hàng";
        }
        return "Báo cáo CRM khách hàng";
    }

    private String reportSubtitle(Authentication authentication) {
        if (hasAnyRole(authentication, "ACCOUNTANT")) {
            return "Theo dõi hóa đơn, trạng thái phát hành, đã thanh toán và công nợ.";
        }
        if (hasAnyRole(authentication, "ADMIN_OFFICER", "ADMINOFFICER")) {
            return "Theo dõi trạng thái hợp đồng, yêu cầu chỉnh sửa và hồ sơ ký kết.";
        }
        if (hasAnyRole(authentication, "DIRECTOR")) {
            return "Tổng hợp hợp đồng, hóa đơn và tiến độ chăm sóc của khách hàng.";
        }
        if (hasAnyRole(authentication, "MARKETING")) {
            return "Theo dõi nguồn khách hàng, trạng thái phân bổ và lịch sử chăm sóc.";
        }
        return "Theo dõi tương tác, báo giá, hợp đồng và tiến độ chuyển đổi.";
    }

    private BigDecimal sumInvoices(List<Invoice> invoices, Invoice.InvoiceStatus status) {
        return invoices.stream()
                .filter(invoice -> status == null || status.equals(invoice.getStatus()))
                .map(invoice -> value(invoice.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal value(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

}
