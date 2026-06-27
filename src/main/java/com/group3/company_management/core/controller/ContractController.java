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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/contracts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES', 'ADMIN_OFFICER', 'ADMINOFFICER', 'MANAGER', 'ADMIN')")
public class ContractController {

    private final ContractService contractService;
    private final CustomerAccountService customerAccountService;

    @GetMapping
    public String listContracts(Authentication authentication, Model model) {
        List<ContractResponse> contracts = List.of();
        List<ContractResponse> pendingAdminContracts = List.of();
        boolean adminOfficerView = hasRole(authentication, "ROLE_ADMIN_OFFICER")
                || hasRole(authentication, "ROLE_ADMINOFFICER")
                || hasRole(authentication, "ROLE_ADMIN");

        try {
            contracts = contractService.getMyContracts(authentication.getName());
            pendingAdminContracts = contractService.getPendingAdminContracts();
        } catch (RuntimeException exception) {
            model.addAttribute("errorMessage", exception.getMessage());
            if (adminOfficerView) {
                pendingAdminContracts = contractService.getPendingAdminContracts();
            }
        }

        model.addAttribute("contracts", contracts);
        model.addAttribute("pendingAdminContracts", pendingAdminContracts);
        model.addAttribute("countContract", contracts.size());
        model.addAttribute("countPendingAdminContract", pendingAdminContracts.size());
        model.addAttribute("signedCount", countByStatus(contracts, "SIGNED"));
        model.addAttribute("processingCount", countProcessing(contracts));
        model.addAttribute("statusClasses", statusClasses());
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
            model.addAttribute("statusClasses", statusClasses());
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

    private long countByStatus(List<ContractResponse> contracts, String status) {
        return contracts.stream()
                .filter(contract -> status.equals(contract.getStatus()))
                .count();
    }

    private long countProcessing(List<ContractResponse> contracts) {
        return contracts.stream()
                .filter(contract -> !"SIGNED".equals(contract.getStatus()))
                .filter(contract -> !"CANCELLED".equals(contract.getStatus()))
                .count();
    }

    private Map<String, String> statusClasses() {
        Map<String, String> classes = new LinkedHashMap<>();
        classes.put("SIGNED", " signed");
        classes.put("PENDING_ADMIN_OFFICER", " pending");
        classes.put("ADMIN_REVIEWED", " reviewed");
        classes.put("SENT_TO_CUSTOMER", " sent");
        classes.put("CANCELLED", " cancelled");
        return classes;
    }

    private ContractRuleRequest toRuleRequest(ContractResponse contract) {
        ContractRuleRequest request = new ContractRuleRequest();
        request.setContractStartDate(contract.getContractStartDate());
        request.setContractEndDate(contract.getContractEndDate());
        request.setPaymentTerms(contract.getPaymentTerms());
        request.setDeliveryTerms(contract.getDeliveryTerms());
        request.setLegalTerms(contract.getLegalTerms());
        request.setAdminNote(contract.getAdminNote());
        request.setSigningDate(contract.getSigningDate());
        request.setSigningPlace(contract.getSigningPlace());
        request.setSellerCompanyName(contract.getSellerCompanyName());
        request.setSellerTaxCode(contract.getSellerTaxCode());
        request.setSellerAddress(contract.getSellerAddress());
        request.setSellerPhone(contract.getSellerPhone());
        request.setSellerFax(contract.getSellerFax());
        request.setSellerBankAccount(contract.getSellerBankAccount());
        request.setSellerBankName(contract.getSellerBankName());
        request.setSellerRepresentativeName(contract.getSellerRepresentativeName());
        request.setSellerRepresentativeTitle(contract.getSellerRepresentativeTitle());
        request.setSellerIdentityNumber(contract.getSellerIdentityNumber());
        request.setSellerIdentityIssuedPlace(contract.getSellerIdentityIssuedPlace());
        request.setSellerIdentityIssuedDate(contract.getSellerIdentityIssuedDate());
        request.setSellerAuthorizationInfo(contract.getSellerAuthorizationInfo());
        request.setBuyerCompanyName(contract.getBuyerCompanyName());
        request.setBuyerTaxCode(contract.getBuyerTaxCode());
        request.setBuyerAddress(contract.getBuyerAddress());
        request.setBuyerPhone(contract.getBuyerPhone());
        request.setBuyerFax(contract.getBuyerFax());
        request.setBuyerBankAccount(contract.getBuyerBankAccount());
        request.setBuyerBankName(contract.getBuyerBankName());
        request.setBuyerRepresentativeName(contract.getBuyerRepresentativeName());
        request.setBuyerRepresentativeTitle(contract.getBuyerRepresentativeTitle());
        request.setBuyerIdentityNumber(contract.getBuyerIdentityNumber());
        request.setBuyerIdentityIssuedPlace(contract.getBuyerIdentityIssuedPlace());
        request.setBuyerIdentityIssuedDate(contract.getBuyerIdentityIssuedDate());
        request.setBuyerAuthorizationInfo(contract.getBuyerAuthorizationInfo());
        request.setAmountInWords(contract.getAmountInWords());
        request.setPaymentDueDate(contract.getPaymentDueDate());
        request.setPaymentMethod(contract.getPaymentMethod());
        request.setDeliverySchedule(contract.getDeliverySchedule());
        request.setShippingResponsibility(contract.getShippingResponsibility());
        request.setUnloadingCost(contract.getUnloadingCost());
        request.setStorageFeePerDay(contract.getStorageFeePerDay());
        request.setInspectionAgency(contract.getInspectionAgency());
        request.setWarrantyProductScope(contract.getWarrantyProductScope());
        request.setWarrantyMonths(contract.getWarrantyMonths());
        request.setPenaltyRate(contract.getPenaltyRate());
        request.setContractCopies(contract.getContractCopies());
        request.setCopiesPerParty(contract.getCopiesPerParty());
        request.setGeneralTerms(contract.getGeneralTerms());
        if (request.getBuyerCompanyName() == null) {
            request.setBuyerCompanyName(contract.getCustomerName());
        }
        if (request.getBuyerAddress() == null) {
            request.setBuyerAddress(contract.getCustomerAddress());
        }
        if (request.getBuyerPhone() == null) {
            request.setBuyerPhone(contract.getCustomerPhone());
        }
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
