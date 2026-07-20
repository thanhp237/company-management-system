package com.group3.company_management.core.controller;

import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.service.PaymentSupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class PaymentSupportController {
    private final PaymentSupportTicketService supportTicketService;

    @GetMapping("/customer/portal/payments/{invoiceId}/support")
    public String showCustomerSupportForm(@PathVariable Long invoiceId,
                                          Authentication authentication,
                                          Model model,
                                          RedirectAttributes redirectAttributes) {
        Customer customer = getCustomer(authentication);
        try {
            model.addAttribute("customer", customer);
            model.addAttribute("invoice", supportTicketService.getCustomerInvoice(invoiceId, customer.getId()));
            return "customer/payment-support-form";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/customer/portal/payments";
        }
    }

    @PostMapping("/customer/portal/payments/{invoiceId}/support")
    public String submitCustomerSupport(@PathVariable Long invoiceId,
                                        @RequestParam String title,
                                        @RequestParam String content,
                                        @RequestParam(required = false) MultipartFile imageFile,
                                        Authentication authentication,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        Customer customer = getCustomer(authentication);
        try {
            supportTicketService.createTicket(invoiceId, customer, title, content, imageFile);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi yêu cầu hỗ trợ thanh toán.");
            return "redirect:/customer/portal/payments";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("customer", customer);
            model.addAttribute("invoice", supportTicketService.getCustomerInvoice(invoiceId, customer.getId()));
            model.addAttribute("titleValue", title);
            model.addAttribute("contentValue", content);
            model.addAttribute("errorMessage", exception.getMessage());
            return "customer/payment-support-form";
        }
    }

    @GetMapping("/support")
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER','ADMINOFFICER','ACCOUNTANT')")
    public String showSupportTickets(Model model) {
        model.addAttribute("tickets", supportTicketService.getTickets());
        return "support/list";
    }

    @PostMapping("/support/{ticketId}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN','ADMIN_OFFICER','ADMINOFFICER','ACCOUNTANT')")
    public String resolveTicket(@PathVariable Long ticketId, RedirectAttributes redirectAttributes) {
        try {
            supportTicketService.resolveTicket(ticketId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đánh dấu yêu cầu là đã xử lý.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/support";
    }

    private Customer getCustomer(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Customer customer)) {
            throw new IllegalArgumentException("Khách hàng chưa đăng nhập.");
        }
        return customer;
    }
}
