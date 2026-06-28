package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.ContractRuleRequest;
import com.group3.company_management.core.dto.ContractResponse;
import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/contracts")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES', 'ADMIN_OFFICER', 'ADMINOFFICER', 'MANAGER', 'ADMIN')")
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    public String listContracts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Contract.ContractStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            Authentication authentication,
            Model model
    ) {
        Page<ContractResponse> contractPage = contractService.searchContracts(
                authentication.getName(), keyword, status, page, size, sortBy, sortDir
        );
        boolean adminOfficerView = hasRole(authentication, "ROLE_ADMIN_OFFICER")
                || hasRole(authentication, "ROLE_ADMINOFFICER");

        model.addAttribute("adminOfficerView", adminOfficerView);

        model.addAttribute("contractPage", contractPage);
        model.addAttribute("contracts", contractPage.getContent());
        model.addAttribute("statistics", contractService.getContractStatistics(authentication.getName()));
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("statuses", Contract.ContractStatus.values());
        model.addAttribute("pendingAdminContracts", List.of());
        model.addAttribute("countPendingAdminContract", 0);
        model.addAttribute("countContract", contractPage.getTotalElements());

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
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('SALES', 'MANAGER', 'ADMIN')")
    public String deleteContract(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            contractService.deleteContract(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Contract deleted successfully.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts";
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
        return request;
    }

    private Map<String, String> statusClasses() {
        return Map.of(
                "DRAFT", " draft",
                "PENDING_ADMIN_OFFICER", " pending",
                "ADMIN_REVIEWED", " reviewed",
                "SENT_TO_CUSTOMER", " sent",
                "SIGNED", " signed",
                "CANCELLED", " cancelled"
        );
    }
}
