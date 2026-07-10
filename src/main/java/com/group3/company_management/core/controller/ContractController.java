package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.ContractRuleRequest;
import com.group3.company_management.core.dto.ContractResponse;
import com.group3.company_management.core.dto.CustomerAccountResult;
import com.group3.company_management.core.dto.PaymentScheduleRequest;
import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.service.ContractService;
import com.group3.company_management.core.service.CustomerAccountService;
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
import java.util.ArrayList;

@Controller
@RequestMapping("/contracts")
@RequiredArgsConstructor

public class ContractController {

    private final ContractService contractService;
    private final CustomerAccountService customerAccountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SALES', 'SALES_MANAGER', 'ADMIN_OFFICER', 'ADMINOFFICER', 'MANAGER', 'ADMIN', 'ACCOUNTANT', 'DIRECTOR')")
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
        List<ContractResponse> pendingAdminContracts = adminOfficerView
                ? contractService.getPendingAdminContracts()
                : List.of();

        model.addAttribute("adminOfficerView", adminOfficerView);

        model.addAttribute("contractPage", contractPage);
        model.addAttribute("contracts", contractPage.getContent());
        model.addAttribute("statistics", contractService.getContractStatistics(authentication.getName()));
        model.addAttribute("keyword", keyword);
        model.addAttribute("status", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("statuses", Contract.ContractStatus.values());
        model.addAttribute("pendingAdminContracts", pendingAdminContracts);
        model.addAttribute("countPendingAdminContract", pendingAdminContracts.size());
        model.addAttribute("countContract", contractPage.getTotalElements());

        return "contracts/list";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SALES', 'SALES_MANAGER', 'ADMIN_OFFICER', 'ADMINOFFICER', 'MANAGER', 'ADMIN', 'ACCOUNTANT', 'DIRECTOR')")
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
    @PreAuthorize("hasAnyRole('SALES', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
    public String createFromQuotation(
            @PathVariable Long quotationId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            Long contractId = contractService.createFromQuotation(quotationId, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Tạo hợp đồng từ báo giá thành công.");
            return "redirect:/contracts/" + contractId;
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/quotation/detail/" + quotationId;
        }
    }

    @PostMapping("/{id}/submit-admin")
    @PreAuthorize("hasAnyRole('SALES', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
    public String submitToAdmin(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            contractService.submitToAdmin(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi hợp đồng cho hành chính hợp đồng.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts/" + id;
    }

    @PostMapping("/{id}/draft-info")
    @PreAuthorize("hasAnyRole('SALES', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
    public String updateDraftInfo(
            @PathVariable Long id,
            @ModelAttribute("contractRuleRequest") ContractRuleRequest request,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            contractService.updateDraftContractInfo(id, request, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu thông tin nháp của hợp đồng.");
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
            redirectAttributes.addFlashAttribute("successMessage", "Đã lưu điều khoản hợp đồng.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts/" + id;
    }

    @PostMapping("/{id}/send-customer")
    @PreAuthorize("hasAnyRole('SALES', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
    public String sendToCustomer(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            contractService.sendToCustomer(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi hợp đồng cho khách hàng.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts/" + id;
    }

    @PostMapping("/{id}/customer-account")
    @PreAuthorize("hasAnyRole('SALES', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
    public String createCustomerAccount(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            CustomerAccountResult result = customerAccountService.createFromContract(id);
            addCustomerAccountMessage(redirectAttributes, result, "Đã cấp account khách hàng.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts/" + id;
    }

    @PostMapping("/{id}/customer-account/resend")
    @PreAuthorize("hasAnyRole('SALES', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
    public String resendCustomerAccount(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {
        try {
            CustomerAccountResult result = customerAccountService.resendAccountEmail(id);
            addCustomerAccountMessage(redirectAttributes, result, "Đã tạo lại mật khẩu account khách hàng.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts/" + id;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('SALES', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
    public String cancelContract(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            contractService.cancelContract(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy hợp đồng.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts/" + id;
    }
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('SALES', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
    public String deleteContract(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            contractService.deleteContract(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy hợp đồng.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/contracts";
    }


    @GetMapping("/export-pdf/{id}")
    @PreAuthorize("hasAnyRole('SALES', 'SALES_MANAGER', 'ADMIN_OFFICER', 'ADMINOFFICER', 'MANAGER', 'ADMIN', 'ACCOUNTANT', 'DIRECTOR','CUSTOMER')")
    public String printPreview(@PathVariable Long id, Model model) {

        ContractResponse contract = contractService.getContractDetail(id);

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

        return "contracts/print";
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
        request.setPaymentPlanType(contract.getPaymentPlanType() == null ? "ONE_TIME" : contract.getPaymentPlanType());
        List<PaymentScheduleRequest> schedules = new ArrayList<>();
        if (contract.getPaymentSchedules() != null) contract.getPaymentSchedules().forEach(item -> {
            PaymentScheduleRequest row = new PaymentScheduleRequest();
            row.setDueDate(item.getDueDate());
            row.setAmount(item.getAmount());
            row.setDescription(item.getDescription());
            schedules.add(row);
        });
        if (schedules.isEmpty()) {
            PaymentScheduleRequest row = new PaymentScheduleRequest();
            row.setDueDate(contract.getPaymentDueDate());
            row.setAmount(contract.getFinalAmount());
            row.setDescription("Thanh toán toàn bộ hợp đồng");
            schedules.add(row);
        }
        while (schedules.size() < 5) schedules.add(new PaymentScheduleRequest());
        request.setPaymentSchedules(schedules);
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
                "REVISION_REQUESTED", " pending",
                "SIGNED", " signed",
                "CANCELLED", " cancelled"
        );
    }

    private void addCustomerAccountMessage(
            RedirectAttributes redirectAttributes,
            CustomerAccountResult result,
            String prefix) {
        if (result.isEmailSent()) {
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    prefix + " Thông tin đăng nhập đã gửi đến " + result.getUsername() + ".");
            return;
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                prefix + " Không gửi được email. Thông tin đăng nhập tạm thời: "
                        + result.getUsername() + " / " + result.getRawPassword());
    }
}
