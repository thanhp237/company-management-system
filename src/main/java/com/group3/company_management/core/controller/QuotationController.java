package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.QuotationRequest;
import com.group3.company_management.core.dto.QuotationResponse;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Opportunity;
import com.group3.company_management.core.repository.CustomerRepository;
import com.group3.company_management.core.repository.OpportunityRepository;
import com.group3.company_management.core.repository.ProductRepository;
import com.group3.company_management.core.repository.QuotationRepository;
import com.group3.company_management.core.repository.VoucherRepository;
import com.group3.company_management.core.service.OpportunityService;
import com.group3.company_management.core.service.QuotationService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/quotation")
@PreAuthorize("hasAnyRole('SALES', 'MANAGER', 'ADMIN')")
public class QuotationController {
    private static final Set<String> QUOTATION_STAGES = Set.of("QUALIFIED", "PROPOSAL", "NEGOTIATION");

    private final VoucherRepository voucherRepository;
    private final QuotationService quotationService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OpportunityRepository opportunityRepository;
    private final QuotationRepository quotationRepository;
    private final OpportunityService opportunityService;

    @GetMapping("/create/{customerId}")
    public String createPage(@PathVariable Long customerId,
                             @RequestParam(required = false) Long opportunityId,
                             Authentication authentication,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        QuotationRequest quotationRequest = new QuotationRequest();
        quotationRequest.setCustomerId(customerId);
        List<Opportunity> opportunities = opportunityRepository.findByCustomerId(customerId)
                .map(List::of)
                .orElseGet(List::of);
        if (opportunityId != null) {
            var opportunity = opportunityService.getOpportunityDetail(opportunityId, authentication.getName());
            if (!customerId.equals(opportunity.getCustomer().getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Opportunity không thuộc khách hàng này.");
                return "redirect:/pipeline/" + opportunityId;
            }
            if (!canCreateQuotation(opportunity)) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Chỉ tạo báo giá khi opportunity ở stage QUALIFIED, PROPOSAL hoặc NEGOTIATION."
                );
                return "redirect:/pipeline/" + opportunityId;
            }
            if (quotationRepository.findFirstByOpportunityIdOrderByCreatedAtDesc(opportunityId).isPresent()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Opportunity này đã có báo giá.");
                return "redirect:/pipeline/" + opportunityId;
            }
            quotationRequest.setOpportunityId(opportunityId);
            opportunities = List.of(opportunity);
        }

        quotationRequest.setQuotationDate(LocalDate.now());
        quotationRequest.setStatus("DRAFT");
        model.addAttribute(
                "vouchers",
                voucherRepository.findByActiveTrueAndExpiredAtAfter(LocalDateTime.now())
        );
        model.addAttribute("quotationRequest", quotationRequest);
        model.addAttribute("customer", customer);
        model.addAttribute("products", productRepository.findByActiveTrue());


        model.addAttribute("quotationCode",
                "QT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        model.addAttribute("opportunities", opportunities);
        model.addAttribute("quotationDate", LocalDate.now());

        model.addAttribute("salesPerson",
                authentication != null ? authentication.getName() : "");

        model.addAttribute("status", "Draft");

        return "quotation/create";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("quotationRequest") QuotationRequest request,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes) {
        if (request.getOpportunityId() != null) {
            try {
                Opportunity opportunity = opportunityService.getOpportunityDetail(request.getOpportunityId(), authentication.getName());
                if (!canCreateQuotation(opportunity)) {
                    redirectAttributes.addFlashAttribute(
                            "errorMessage",
                            "Không thể tạo báo giá cho opportunity ở stage hiện tại."
                    );
                    return "redirect:/pipeline/" + request.getOpportunityId();
                }
                if (quotationRepository.findFirstByOpportunityIdOrderByCreatedAtDesc(request.getOpportunityId()).isPresent()) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Opportunity này đã có báo giá.");
                    return "redirect:/pipeline/" + request.getOpportunityId();
                }
                request.setCustomerId(opportunity.getCustomer().getId());
            } catch (IllegalArgumentException exception) {
                redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
                return "redirect:/pipeline";
            }
        }

        Long quotationId = quotationService.createQuotation(request, authentication.getName());

        return "redirect:/quotation/detail/" + quotationId;
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id,
                         Model model) {

        QuotationResponse quotation =
                quotationService.getQuotationDetail(id);

        model.addAttribute("quotation", quotation);

        return "quotation/detail";
    }


    private boolean canCreateQuotation(Opportunity opportunity) {
        return opportunity != null
                && opportunity.getStage() != null
                && QUOTATION_STAGES.contains(opportunity.getStage().toUpperCase());
    }
}
