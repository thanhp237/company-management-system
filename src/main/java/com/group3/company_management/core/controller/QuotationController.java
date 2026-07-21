package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.QuotationDetailRequest;
import com.group3.company_management.core.dto.QuotationRequest;
import com.group3.company_management.core.dto.QuotationResponse;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Product;
import com.group3.company_management.core.repository.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/quotation")
@PreAuthorize("hasAnyRole('SALES', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
public class QuotationController {
    private final VoucherRepository voucherRepository;
    private final QuotationService quotationService;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;
    @GetMapping("/create/{customerId}")
    public String createPage(@PathVariable Long customerId,
                             @RequestParam(required = false) Long opportunityId,
                             Authentication authentication,
                             Model model) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        QuotationRequest quotationRequest = new QuotationRequest();
        quotationRequest.setCustomerId(customerId);
        quotationRequest.setOpportunityId(opportunityId);
        quotationRequest.setQuotationDate(LocalDate.now());
        quotationRequest.setStatus("DRAFT");
        List<Product> products = productRepository.findByActiveTrue();
        List<QuotationDetailRequest> details = new ArrayList<>();


        for (Product p : products) {
            QuotationDetailRequest item = new QuotationDetailRequest();
            item.setProductId(p.getId());
            item.setQuantity(1);
            item.setSelected(false);
            details.add(item);
        }
        quotationRequest.setDetails(details);

        model.addAttribute(
                "vouchers",
                voucherRepository.findUsableVouchers(LocalDateTime.now())
        );
        model.addAttribute("quotationRequest", quotationRequest);
        model.addAttribute("customer", customer);
        model.addAttribute("products", products);

        String quotationCode = "QT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        quotationRequest.setQuotationCode(quotationCode);
        model.addAttribute("quotationCode", quotationCode);

        model.addAttribute("opportunities", opportunityRepository.findFirstOpportunityByCustomerId(customerId));
        model.addAttribute("quotationDate", LocalDate.now());

        model.addAttribute("salesPerson", authentication != null ? authentication.getName() : "");
        model.addAttribute("status", "Draft");


        return "quotation/create";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("quotationRequest") QuotationRequest request,
                       Authentication authentication,
                       RedirectAttributes redirectAttributes) {

        try {
            Long quotationId = quotationService.createQuotation(request, authentication.getName());
            return "redirect:/quotation/detail/" + quotationId;
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/quotation/create/" + request.getCustomerId()
                    + (request.getOpportunityId() == null ? "" : "?opportunityId=" + request.getOpportunityId());
        }

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
    @PreAuthorize("hasAnyRole('SALES', 'SALES_MANAGER', 'MANAGER', 'ADMIN')")
    public String acceptQuotation(@PathVariable Long id,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            quotationService.acceptQuotation(id, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt báo giá thành công.");
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }

        return "redirect:/quotation/detail/" + id;
    }
    @GetMapping("/export-pdf/{id}")
    public String exportPdf(@PathVariable Long id, Model model, Authentication authentication) {
        QuotationResponse quotation = quotationService.getQuotationDetail(id);
        model.addAttribute("quotation", quotation);

        String approverName = "Ban Giám Đốc";
        
        // 1. Lấy Tên đầy đủ của người duyệt báo giá thực tế
        if (quotation.getApprovedBy() != null) {
            var approvedUserOpt = userRepository.findByUsername(quotation.getApprovedBy());
            if (approvedUserOpt.isPresent()) {
                approverName = approvedUserOpt.get().getFullName();
            } else {
                approverName = quotation.getApprovedBy();
            }
        } else if (authentication != null) {
           
            var currentUserOpt = userRepository.findByUsername(authentication.getName());
            if (currentUserOpt.isPresent()) {
                approverName = currentUserOpt.get().getFullName();
            } else {
                approverName = authentication.getName();
            }
        }
        model.addAttribute("approverName", approverName != null ? approverName : "Ban Giám Đốc");

        return "quotation/print";
    }



}
