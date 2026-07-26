package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.QuotationDetailRequest;
import com.group3.company_management.core.dto.QuotationRequest;
import com.group3.company_management.core.dto.QuotationResponse;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Product;
import com.group3.company_management.core.entity.User;
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
import java.util.Optional;
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
        model.addAttribute("isEdit", false);

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

    @GetMapping("/edit/{id}")
    public String editPage(@PathVariable Long id,
                           Authentication authentication,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            QuotationResponse quotation = quotationService.getQuotationDetail(id);

            if (quotation.getStatus() == null || !"DRAFT".equalsIgnoreCase(quotation.getStatus())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Chỉ báo giá ở trạng thái nháp (DRAFT) mới có thể chỉnh sửa.");
                return "redirect:/quotation/detail/" + id;
            }

            Customer customer = customerRepository.findById(quotation.getCustomerId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

            QuotationRequest quotationRequest = new QuotationRequest();
            quotationRequest.setCustomerId(quotation.getCustomerId());
            quotationRequest.setOpportunityId(quotation.getOpportunityId());
            quotationRequest.setQuotationCode(quotation.getQuotationCode());
            quotationRequest.setValidUntil(quotation.getValidUntil());
            quotationRequest.setVoucherId(quotation.getVoucherId());
            quotationRequest.setNote(quotation.getNote());
            quotationRequest.setStatus(quotation.getStatus());

            List<Product> products = productRepository.findByActiveTrue();
            List<QuotationDetailRequest> details = new ArrayList<>();

            for (Product p : products) {
                QuotationDetailRequest item = new QuotationDetailRequest();
                item.setProductId(p.getId());

                Optional<com.group3.company_management.core.dto.QuotationDetailResponse> existingDetail =
                        quotation.getDetails() != null ?
                        quotation.getDetails().stream()
                                .filter(d -> d.getProductId() != null && d.getProductId().equals(p.getId()))
                                .findFirst() : Optional.empty();

                if (existingDetail.isPresent()) {
                    item.setSelected(true);
                    item.setQuantity(existingDetail.get().getQuantity() != null ? existingDetail.get().getQuantity() : 1);
                } else {
                    item.setSelected(false);
                    item.setQuantity(1);
                }
                details.add(item);
            }
            quotationRequest.setDetails(details);

            model.addAttribute("vouchers", voucherRepository.findUsableVouchers(LocalDateTime.now()));
            model.addAttribute("quotationRequest", quotationRequest);
            model.addAttribute("quotationId", id);
            model.addAttribute("customer", customer);
            model.addAttribute("products", products);
            model.addAttribute("quotationCode", quotation.getQuotationCode());
            model.addAttribute("opportunities", opportunityRepository.findFirstOpportunityByCustomerId(quotation.getCustomerId()));
            model.addAttribute("quotationDate", quotation.getCreatedAt() != null ? quotation.getCreatedAt().toLocalDate() : LocalDate.now());
            model.addAttribute("salesPerson", authentication != null ? authentication.getName() : "");
            model.addAttribute("status", quotation.getStatus());
            model.addAttribute("isEdit", true);

            return "quotation/create";
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/quotation/detail/" + id;
        }
    }

    @PostMapping("/update/{id}")
    public String update(@PathVariable Long id,
                         @ModelAttribute("quotationRequest") QuotationRequest request,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            Long quotationId = quotationService.updateQuotation(id, request, authentication.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật báo giá thành công.");
            return "redirect:/quotation/detail/" + quotationId;
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/quotation/edit/" + id;
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
        

        if (quotation.getApprovedBy() != null) {
            Optional<User> approvedUserOpt = userRepository.findByUsername(quotation.getApprovedBy());
            if (approvedUserOpt.isPresent()) {
                approverName = approvedUserOpt.get().getFullName();
            } else {
                approverName = quotation.getApprovedBy();
            }
        } else if (authentication != null) {

            Optional<User> currentUserOpt = userRepository.findByUsername(authentication.getName());
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
