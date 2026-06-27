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
        return request;
    }
}
