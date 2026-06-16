package com.group3.company_management.core.controller;

// src/main/java/com/group3/company_management/customer/controller/CustomerPortalController.java



import com.group3.company_management.customer.dto.CustomerPortalResponse;
import com.group3.company_management.core.dto.CustomerRequest;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.service.CustomerService;
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
        
        model.addAttribute("title", "My Account");
        model.addAttribute("customer", customer);
        model.addAttribute("portalInfo", portalInfo);
        
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
        
        model.addAttribute("title", "My Profile");
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
            model.addAttribute("successMessage", "Profile updated successfully");
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
        
        model.addAttribute("title", "My Contracts");
        model.addAttribute("customer", customer);
        // TODO: Fetch contracts from ContractService
        
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
        
        model.addAttribute("title", "Contract Details");
        model.addAttribute("customer", customer);
        model.addAttribute("contractId", contractId);
        // TODO: Fetch contract from ContractService
        
        log.info("✅ Customer contract detail accessed: {} - Contract ID: {}", customer.getEmail(), contractId);
        return "customer/contract-detail";
    }
    
    /**
     * GET /customer/portal/quotes
     * View customer's quotes
     */
    @GetMapping("/quotes")
    public String showCustomerQuotes(Model model, Authentication authentication) {
        Customer customer = getAuthenticatedCustomer(authentication);
        
        model.addAttribute("title", "My Quotes");
        model.addAttribute("customer", customer);
        // TODO: Fetch quotes from QuoteService
        
        log.info("✅ Customer quotes page accessed: {}", customer.getEmail());
        return "customer/quotes";
    }
    
    /**
     * GET /customer/portal/quotes/{quoteId}
     * View specific quote
     */
    @GetMapping("/quotes/{quoteId}")
    public String showQuoteDetail(
            @PathVariable Long quoteId,
            Model model,
            Authentication authentication) {
        
        Customer customer = getAuthenticatedCustomer(authentication);
        
        model.addAttribute("title", "Quote Details");
        model.addAttribute("customer", customer);
        model.addAttribute("quoteId", quoteId);
        // TODO: Fetch quote from QuoteService
        
        log.info("✅ Customer quote detail accessed: {} - Quote ID: {}", customer.getEmail(), quoteId);
        return "customer/quote-detail";
    }
    
    /**
     * POST /customer/portal/quotes/{quoteId}/accept
     * Accept a quote
     */
    @PostMapping("/quotes/{quoteId}/accept")
    public String acceptQuote(
            @PathVariable Long quoteId,
            Authentication authentication) {
        
        Customer customer = getAuthenticatedCustomer(authentication);
        log.info("Customer accepting quote - Customer: {}, Quote ID: {}", customer.getEmail(), quoteId);
        
        try {
            // TODO: Call QuoteService.acceptQuote()
            return "redirect:/customer/portal/quotes/" + quoteId + "?accepted=true";
        } catch (Exception e) {
            log.error("Error accepting quote: {}", e.getMessage());
            return "redirect:/customer/portal/quotes/" + quoteId + "?error=true";
        }
    }
    
    /**
     * POST /customer/portal/quotes/{quoteId}/reject
     * Reject a quote
     */
    @PostMapping("/quotes/{quoteId}/reject")
    public String rejectQuote(
            @PathVariable Long quoteId,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        
        Customer customer = getAuthenticatedCustomer(authentication);
        log.info("Customer rejecting quote - Customer: {}, Quote ID: {}", customer.getEmail(), quoteId);
        
        try {
            // TODO: Call QuoteService.rejectQuote()
            return "redirect:/customer/portal/quotes?rejected=true";
        } catch (Exception e) {
            log.error("Error rejecting quote: {}", e.getMessage());
            return "redirect:/customer/portal/quotes/" + quoteId + "?error=true";
        }
    }
    
    /**
     * GET /customer/portal/payments
     * View payment history
     */
    @GetMapping("/payments")
    public String showCustomerPayments(Model model, Authentication authentication) {
        Customer customer = getAuthenticatedCustomer(authentication);
        
        model.addAttribute("title", "Payment History");
        model.addAttribute("customer", customer);
        // TODO: Fetch payments from PaymentService
        
        log.info("✅ Customer payments page accessed: {}", customer.getEmail());
        return "customer/payments";
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
        throw new RuntimeException("Customer not authenticated");
    }
}