package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.QuotationRequest;
import com.group3.company_management.core.dto.QuotationResponse;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.repository.CustomerRepository;
import com.group3.company_management.core.repository.OpportunityRepository;
import com.group3.company_management.core.repository.ProductRepository;
import com.group3.company_management.core.repository.VoucherRepository;
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
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/quotation")
public class QuotationController {
    private final VoucherRepository voucherRepository;
    private final QuotationService quotationService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OpportunityRepository opportunityRepository;
    @GetMapping("/create/{customerId}")
    public String createPage(@PathVariable Long customerId,
                             @RequestParam(required = false) Long opportunityId,
                             Authentication authentication,
                             Model model) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        QuotationRequest quotationRequest = new QuotationRequest();
        quotationRequest.setCustomerId(customerId);
        quotationRequest.setOpportunityId(opportunityId);

        quotationRequest.setQuotationDate(LocalDate.now());
        quotationRequest.setStatus("DRAFT");
        model.addAttribute(
                "vouchers",
                voucherRepository.findByActiveTrueAndExpiredAtAfter(LocalDateTime.now())
        );
        model.addAttribute("quotationRequest", quotationRequest);
        model.addAttribute("customer", customer);
        model.addAttribute("products", productRepository.findByActiveTrue());


        String quotationCode = "QT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        quotationRequest.setQuotationCode(quotationCode);
        model.addAttribute("quotationCode", quotationCode);
        model.addAttribute("opportunities",
                opportunityRepository.findFirstOpportunityByCustomerId(customerId));
        model.addAttribute("quotationDate", LocalDate.now());

        model.addAttribute("salesPerson",
                authentication != null ? authentication.getName() : "");

        model.addAttribute("status", "Draft");

        return "quotation/create";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("quotationRequest") QuotationRequest request,
                       Authentication authentication) {

        Long quotationId = quotationService.createQuotation(request, authentication.getName());

        return "redirect:/quotation/detail/" + quotationId;
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id,
                         Model model) {

        QuotationResponse quotation =
                quotationService.getQuotationDetail(id);

        model.addAttribute("quotation", quotation);
        model.addAttribute("backPipelineId", quotation.getOpportunityId());

        return "quotation/detail";
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('SALES', 'MANAGER', 'ADMIN')")
    public String acceptQuotation(@PathVariable Long id,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            quotationService.acceptQuotation(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Quotation accepted successfully.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/quotation/detail/" + id;
    }



}
