package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.ContractRuleRequest;
import com.group3.company_management.core.dto.ContractResponse;
import com.group3.company_management.core.dto.CustomerAccountResult;
import com.group3.company_management.core.service.CustomerAccountService;
import com.group3.company_management.core.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/contracts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES', 'ADMIN_OFFICER', 'ADMINOFFICER', 'MANAGER', 'ADMIN')")
public class ContractController {

    private final ContractService contractService;
    private final CustomerAccountService customerAccountService;

    @GetMapping
    public String listContracts(Authentication authentication, Model model) {
        List<ContractResponse> contracts = contractService.getMyContracts(authentication.getName());
        List<ContractResponse> pendingAdminContracts = contractService.getPendingAdminContracts();
        boolean adminOfficerView = hasRole(authentication, "ROLE_ADMIN_OFFICER")
                || hasRole(authentication, "ROLE_ADMINOFFICER")
                || hasRole(authentication, "ROLE_ADMIN");

        model.addAttribute("contracts", contracts);
        model.addAttribute("pendingAdminContracts", pendingAdminContracts);
        model.addAttribute("countContract", contracts.size());
        model.addAttribute("countPendingAdminContract", pendingAdminContracts.size());
        model.addAttribute("adminOfficerView", adminOfficerView);

        return "contracts/list";
    }

    @GetMapping("/{id}")
    public String detailContract(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            ContractResponse contract = contractService.getContractDetail(id);
            model.addAttribute("contract", contract);
            model.addAttribute("contractRuleRequest", toRuleRequest(contract));
            return "contracts/contract";
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/contracts";
        }
    }

    @PostMapping("/create-from-quotation/{quotationId}")
    @PreAuthorize("hasAnyRole('SALES', 'MANAGER', 'ADMIN')")
    public String createFromQuotation(
            @PathVariable Long quotationId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Long contractId = contractService.createFromQuotation(quotationId, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Contract created from quotation successfully.");
            return "redirect:/contracts/" + contractId;
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/quotation/detail/" + quotationId;
        }
    }

    @PostMapping("/{id}/submit-admin")
    @PreAuthorize("hasAnyRole('SALES', 'MANAGER', 'ADMIN')")
    public String submitToAdmin(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            contractService.submitToAdmin(id);
            redirectAttributes.addFlashAttribute("successMessage", "Contract submitted to admin officer.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts/" + id;
    }

    @PostMapping("/{id}/rules")
    @PreAuthorize("hasAnyRole('ADMIN_OFFICER', 'ADMINOFFICER', 'ADMIN')")
    public String updateContractRules(
            @PathVariable Long id,
            @ModelAttribute("contractRuleRequest") ContractRuleRequest request,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            contractService.updateContractRules(id, request, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Contract rules saved successfully.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts/" + id;
    }

    @PostMapping("/{id}/send-customer")
    @PreAuthorize("hasAnyRole('SALES', 'MANAGER', 'ADMIN')")
    public String sendToCustomer(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            contractService.sendToCustomer(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Contract sent to customer.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts/" + id;
    }

    @PostMapping("/{id}/customer-sign")
    @PreAuthorize("hasAnyRole('SALES', 'MANAGER', 'ADMIN')")
    public String customerSignContract(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            contractService.customerSignContract(id);
            redirectAttributes.addFlashAttribute("successMessage", "Contract signed by customer.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts/" + id;
    }

    @PostMapping("/{id}/create-customer-account")
    @PreAuthorize("hasAnyRole('SALES', 'ADMIN_OFFICER', 'ADMINOFFICER', 'MANAGER', 'ADMIN')")
    public String createCustomerAccount(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            CustomerAccountResult result = customerAccountService.createFromContract(id);
            addCustomerAccountMessage(redirectAttributes, result, "Customer account created");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts/" + id;
    }

    @PostMapping("/{id}/resend-customer-account-email")
    @PreAuthorize("hasAnyRole('SALES', 'ADMIN_OFFICER', 'ADMINOFFICER', 'MANAGER', 'ADMIN')")
    public String resendCustomerAccountEmail(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            CustomerAccountResult result = customerAccountService.resendAccountEmail(id);
            addCustomerAccountMessage(redirectAttributes, result, "Customer login email resent");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts/" + id;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SALES', 'MANAGER', 'ADMIN')")
    public String cancelContract(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            contractService.cancelContract(id);
            redirectAttributes.addFlashAttribute("successMessage", "Contract cancelled successfully.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts/" + id;
    }

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    private ContractRuleRequest toRuleRequest(ContractResponse contract) {
        ContractRuleRequest request = new ContractRuleRequest();
        request.setContractStartDate(contract.getContractStartDate());
        request.setContractEndDate(contract.getContractEndDate());
        request.setPaymentTerms(contract.getPaymentTerms());
        request.setDeliveryTerms(contract.getDeliveryTerms());
        request.setLegalTerms(contract.getLegalTerms());
        request.setAdminNote(contract.getAdminNote());
        return request;
    }

    private void addCustomerAccountMessage(
            RedirectAttributes redirectAttributes,
            CustomerAccountResult result,
            String actionMessage) {
        if (result.isEmailSent()) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    actionMessage + " and email sent successfully"
            );
            return;
        }

        redirectAttributes.addFlashAttribute(
                "errorMessage",
                actionMessage + ", but email sending failed. Temporary login: "
                        + result.getUsername() + " / " + result.getRawPassword()
                        + ". Please check SMTP username/password in application.properties."
        );
    }
}
