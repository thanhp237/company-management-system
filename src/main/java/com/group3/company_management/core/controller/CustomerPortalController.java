package com.group3.company_management.core.controller;

// src/main/java/com/group3/company_management/customer/controller/CustomerPortalController.java



import com.group3.company_management.customer.dto.CustomerPortalResponse;
import com.group3.company_management.core.dto.ContractResponse;
import com.group3.company_management.core.dto.CustomerRequest;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.service.ContractService;
import com.group3.company_management.core.service.CustomerService;
import com.group3.company_management.core.service.NotificationService;
import com.group3.company_management.core.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Customer portal controller
 * Dashboard and profile management for logged-in customers
 * Spring Security 6+ authentication required
 */
@Controller
@RequestMapping("/customer/portal")
@RequiredArgsConstructor
@Slf4j
public class CustomerPortalController {
    
    private final CustomerService customerService;
    private final ContractService contractService;
    private final InvoiceRepository invoiceRepository;
    private final NotificationService notificationService;
    
    /**
     * GET /customer/portal
     * Customer home/dashboard page
     * Shows contracts, quotes, payment status
     */
    @GetMapping
    public String showCustomerPortal(Model model, Authentication authentication) {
        Customer customer = getAuthenticatedCustomer(authentication);
        
        // Get portal info with metrics
        CustomerPortalResponse portalInfo = customerService.getCustomerPortalInfo(customer.getId());
        
        model.addAttribute("title", "Tài khoản của tôi");
        model.addAttribute("customer", customer);
        model.addAttribute("portalInfo", portalInfo);
        model.addAttribute("contracts", contractService.getCustomerContracts(customer.getId()));
        
        log.info("✅ Customer portal accessed: {}", customer.getEmail());
        return "customer/portal";
    }
    
    /**
     * GET /customer/portal/profile
     * Customer profile edit page
     */
    @GetMapping("/profile")
    public String showCustomerProfile(Model model, Authentication authentication) {
        Customer customer = getAuthenticatedCustomer(authentication);
        
        model.addAttribute("title", "Hồ sơ của tôi");
        model.addAttribute("customer", customer);
        
        log.info("✅ Customer profile page accessed: {}", customer.getEmail());
        return "customer/profile";
    }
    
    /**
     * POST /customer/portal/profile/update
     * Update customer profile (limited fields)
     */
    @PostMapping("/profile/update")
    public String updateCustomerProfile(
            @ModelAttribute CustomerRequest request,
            Model model,
            Authentication authentication) {
        
        Customer customer = getAuthenticatedCustomer(authentication);
        log.info("Updating customer profile: {}", customer.getEmail());
        
        try {
            request.setId(customer.getId());
            customerService.updateCustomerProfile(customer.getId(), request);
            model.addAttribute("successMessage", "Cập nhật hồ sơ thành công.");
            return "redirect:/customer/portal/profile?success=true";
        } catch (Exception e) {
            log.error("Error updating customer profile: {}", e.getMessage());
            model.addAttribute("errorMessage", e.getMessage());
            return "customer/profile";
        }
    }
    
    /**
     * GET /customer/portal/contracts
     * View customer's contracts
     */
    @GetMapping("/contracts")
    public String showCustomerContracts(Model model, Authentication authentication) {
        Customer customer = getAuthenticatedCustomer(authentication);
        
        model.addAttribute("title", "Hợp đồng của tôi");
        model.addAttribute("customer", customer);
        model.addAttribute("contracts", contractService.getCustomerContracts(customer.getId()));
        
        log.info("✅ Customer contracts page accessed: {}", customer.getEmail());
        return "customer/contracts";
    }
    
    /**
     * GET /customer/portal/contracts/{contractId}
     * View specific contract details
     */
    @GetMapping("/contracts/{contractId}")
    public String showContractDetail(
            @PathVariable Long contractId,
            Model model,
            Authentication authentication) {
        
        Customer customer = getAuthenticatedCustomer(authentication);
        ContractResponse contract = contractService.getCustomerContractDetail(contractId, customer.getId());
        
        model.addAttribute("title", "Chi tiết hợp đồng");
        model.addAttribute("customer", customer);
        model.addAttribute("contract", contract);
        
        log.info("✅ Customer contract detail accessed: {} - Contract ID: {}", customer.getEmail(), contractId);
        return "customer/contract-detail";
    }

    @PostMapping("/contracts/{contractId}/sign")
    public String signContract(
            @PathVariable Long contractId,
            Authentication authentication,
            Model model) {

        Customer customer = getAuthenticatedCustomer(authentication);
        log.info("Customer signing contract - Customer: {}, Contract ID: {}", customer.getEmail(), contractId);

        try {
            contractService.customerSignContractByCustomer(contractId, customer.getId());
            return "redirect:/customer/portal/contracts/" + contractId + "?signed=true";
        } catch (RuntimeException exception) {
            ContractResponse contract = contractService.getCustomerContractDetail(contractId, customer.getId());
            model.addAttribute("title", "Chi tiết hợp đồng");
            model.addAttribute("customer", customer);
            model.addAttribute("contract", contract);
            model.addAttribute("errorMessage", exception.getMessage());
            return "customer/contract-detail";
        }
    }

    @PostMapping("/contracts/{contractId}/request-revision")
    public String requestRevision(@PathVariable Long contractId, @RequestParam String reason,
                                  Authentication authentication) {
        Customer customer = getAuthenticatedCustomer(authentication);
        contractService.customerRequestRevision(contractId, customer.getId(), reason);
        return "redirect:/customer/portal/contracts/" + contractId + "?revisionRequested=true";
    }
    
    @RequestMapping("/quotes/**")
    public String redirectLegacyQuotePages() {
        return "redirect:/customer/portal/contracts";
    }
    
    /**
     * GET /customer/portal/payments
     * View payment history
     */
    @GetMapping("/payments")
    public String showCustomerPayments(Model model, Authentication authentication) {
        Customer customer = getAuthenticatedCustomer(authentication);
        
        model.addAttribute("title", "Lịch sử thanh toán");
        model.addAttribute("customer", customer);
        
        java.util.List<com.group3.company_management.core.entity.Invoice> invoices = invoiceRepository.findByContractCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream()
                .filter(inv -> inv.getStatus() != com.group3.company_management.core.entity.Invoice.InvoiceStatus.DRAFT)
                .collect(java.util.stream.Collectors.toList());
        model.addAttribute("invoices", invoices);
        
        log.info("✅ Customer payments page accessed: {}", customer.getEmail());
        return "customer/payments";
    }

    @GetMapping("/notifications")
    public String showCustomerNotifications(Model model, Authentication authentication) {
        Customer customer = getAuthenticatedCustomer(authentication);
        
        model.addAttribute("title", "Thông báo của tôi");
        model.addAttribute("customer", customer);
        model.addAttribute("notifications", notificationService.getNotificationsByCustomerId(customer.getId()));
        
        log.info("✅ Customer notifications page accessed: {}", customer.getEmail());
        return "customer/notifications";
    }
    // Endpoint xuất PDF dành riêng cho Khách hàng
    @GetMapping("/contracts/{contractId}/export-pdf")
    public String customerPrintPreview(
            @PathVariable Long contractId,
            Model model,
            Authentication authentication) {

        Customer customer = getAuthenticatedCustomer(authentication);
        ContractResponse contract = contractService.getCustomerContractDetail(contractId, customer.getId());

        // Fallbacks thông tin khách hàng nếu chưa lưu nháp hợp đồng
        String buyerCompanyName = contract.getBuyerCompanyName();
        if (buyerCompanyName == null || buyerCompanyName.trim().isEmpty()) {
            buyerCompanyName = contract.getCustomerName();
        }

        String buyerPhone = contract.getBuyerPhone();
        if (buyerPhone == null || buyerPhone.trim().isEmpty()) {
            buyerPhone = contract.getCustomerPhone();
        }

        String buyerAddress = contract.getBuyerAddress();
        if (buyerAddress == null || buyerAddress.trim().isEmpty()) {
            buyerAddress = contract.getCustomerAddress();
        }

        String buyerRepresentativeName = contract.getBuyerRepresentativeName();
        if (buyerRepresentativeName == null || buyerRepresentativeName.trim().isEmpty()) {
            buyerRepresentativeName = contract.getCustomerName();
        }

        String buyerRepresentativeTitle = contract.getBuyerRepresentativeTitle();
        if (buyerRepresentativeTitle == null || buyerRepresentativeTitle.trim().isEmpty()) {
            buyerRepresentativeTitle = "";
        }

        String buyerTaxCode = contract.getBuyerTaxCode();

        model.addAttribute("contract", contract);
        model.addAttribute("buyerCompanyName", buyerCompanyName);
        model.addAttribute("buyerTaxCode", buyerTaxCode);
        model.addAttribute("buyerAddress", buyerAddress);
        model.addAttribute("buyerPhone", buyerPhone);
        model.addAttribute("buyerRepresentativeName", buyerRepresentativeName);
        model.addAttribute("buyerRepresentativeTitle", buyerRepresentativeTitle);

        // Đường dẫn quay lại dành cho Khách hàng
        model.addAttribute("backUrl", "/customer/portal/contracts/" + contractId);

        return "contracts/print";
    }
    
    // ============= Helper Methods =============
    
    /**
     * Get authenticated customer from security context
     */
    private Customer getAuthenticatedCustomer(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Customer)) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        
        if (authentication != null && authentication.getPrincipal() instanceof Customer) {
            return (Customer) authentication.getPrincipal();
        }
        
        log.error("❌ Unauthenticated or invalid customer session");
        throw new RuntimeException("Khách hàng chưa đăng nhập.");
    }
}
