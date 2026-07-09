package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.CreateInvoiceRequest;
import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.entity.Invoice;
import com.group3.company_management.core.repository.ContractRepository;
import com.group3.company_management.core.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/invoices")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SALES', 'SALES_MANAGER', 'ADMIN_OFFICER', 'ADMIN', 'MANAGER')")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final ContractRepository contractRepository;


    @GetMapping
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
                       @RequestParam(required = false, defaultValue = "desc") String order,
                       Model model) {

        List<Invoice> allInvoices = invoiceService.getAllInvoices();
        long totalCount = allInvoices.size();
        long draftCount = allInvoices.stream().filter(inv -> Invoice.InvoiceStatus.DRAFT.equals(inv.getStatus())).count();
        long issuedCount = allInvoices.stream().filter(inv -> Invoice.InvoiceStatus.ISSUED.equals(inv.getStatus())).count();
        long cancelledCount = allInvoices.stream().filter(inv -> Invoice.InvoiceStatus.CANCELLED.equals(inv.getStatus())).count();

        List<Invoice> filteredInvoices = invoiceService.getAllInvoicesFiltered(search, status, sortBy, order);

        model.addAttribute("invoices", filteredInvoices);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("draftCount", draftCount);
        model.addAttribute("issuedCount", issuedCount);
        model.addAttribute("cancelledCount", cancelledCount);

        model.addAttribute("search", search);
        model.addAttribute("status", status);
        model.addAttribute("sortBy", sortBy);
        model.addAttribute("order", order);

        return "Invoice/list";
    }

    @GetMapping("/create/{contractId}")
    public String createForm(@PathVariable Long contractId, Model model) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng"));


        String customerName = "N/A";
        if (contract.getCustomer() != null) {
            customerName = contract.getCustomer().getFullName();
            if (customerName == null || customerName.trim().isEmpty()) {
                customerName = contract.getCustomer().getName();
            }
            if (customerName == null || customerName.trim().isEmpty()) {
                customerName = contract.getCustomer().getCompanyName();
            }
        } else if (contract.getBuyerCompanyName() != null) {
            customerName = contract.getBuyerCompanyName();
        }

        String customerTaxCode = "N/A";
        if (contract.getCustomer() != null && contract.getCustomer().getTaxCode() != null) {
            customerTaxCode = contract.getCustomer().getTaxCode();
        } else if (contract.getBuyerTaxCode() != null) {
            customerTaxCode = contract.getBuyerTaxCode();
        }

        String customerAddress = "N/A";
        if (contract.getCustomer() != null && contract.getCustomer().getAddress() != null) {
            customerAddress = contract.getCustomer().getAddress();
        } else if (contract.getBuyerAddress() != null) {
            customerAddress = contract.getBuyerAddress();
        }

        model.addAttribute("contractId", contractId);
        model.addAttribute("contract", contract);
        model.addAttribute("customerName", customerName);
        model.addAttribute("customerTaxCode", customerTaxCode);
        model.addAttribute("customerAddress", customerAddress);
        model.addAttribute("invoiceCode", invoiceService.generateNextInvoiceCode());
        model.addAttribute("invoiceForm", invoiceService.prepareCreateRequest(contractId));
        return "Invoice/invoice-form";
    }

    @PostMapping("/create/{contractId}")
    public String create(@PathVariable Long contractId,
                         @ModelAttribute("invoiceForm") CreateInvoiceRequest request,
                         RedirectAttributes ra) {
        try {
            Invoice invoice = invoiceService.createInvoice(contractId, request);
            if (Invoice.InvoiceStatus.ISSUED.equals(invoice.getStatus())) {
                ra.addFlashAttribute("successMessage", "Hóa đơn đã được phát hành thành công! Mã: " + invoice.getInvoiceCode());
            } else {
                ra.addFlashAttribute("successMessage", "Lưu hóa đơn nháp thành công! Mã: " + invoice.getInvoiceCode());
            }
            return "redirect:/contracts/" + contractId;
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/invoices/create/" + contractId;
        }
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Invoice invoice = invoiceService.getInvoiceById(id);
        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new RuntimeException("Chỉ hóa đơn DRAFT mới có thể chỉnh sửa");
        }
        Contract contract = invoice.getContract();


        String customerName = "N/A";
        if (contract.getCustomer() != null) {
            customerName = contract.getCustomer().getFullName();
            if (customerName == null || customerName.trim().isEmpty()) {
                customerName = contract.getCustomer().getName();
            }
            if (customerName == null || customerName.trim().isEmpty()) {
                customerName = contract.getCustomer().getCompanyName();
            }
        } else if (contract.getBuyerCompanyName() != null) {
            customerName = contract.getBuyerCompanyName();
        }

        String customerTaxCode = "N/A";
        if (contract.getCustomer() != null && contract.getCustomer().getTaxCode() != null) {
            customerTaxCode = contract.getCustomer().getTaxCode();
        } else if (contract.getBuyerTaxCode() != null) {
            customerTaxCode = contract.getBuyerTaxCode();
        }

        String customerAddress = "N/A";
        if (contract.getCustomer() != null && contract.getCustomer().getAddress() != null) {
            customerAddress = contract.getCustomer().getAddress();
        } else if (contract.getBuyerAddress() != null) {
            customerAddress = contract.getBuyerAddress();
        }

        model.addAttribute("invoiceId", id);
        model.addAttribute("contractId", contract.getId());
        model.addAttribute("contract", contract);
        model.addAttribute("customerName", customerName);
        model.addAttribute("customerTaxCode", customerTaxCode);
        model.addAttribute("customerAddress", customerAddress);
        model.addAttribute("invoiceCode", invoice.getInvoiceCode());
        model.addAttribute("invoiceForm", invoiceService.prepareEditRequest(id));
        return "Invoice/invoice-form";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                       @ModelAttribute("invoiceForm") CreateInvoiceRequest request,
                       RedirectAttributes ra) {
        try {
            Invoice invoice = invoiceService.updateInvoice(id, request);
            if (Invoice.InvoiceStatus.ISSUED.equals(invoice.getStatus())) {
                ra.addFlashAttribute("successMessage", "Hóa đơn đã được phát hành thành công! Mã: " + invoice.getInvoiceCode());
            } else {
                ra.addFlashAttribute("successMessage", "Cập nhật hóa đơn nháp thành công! Mã: " + invoice.getInvoiceCode());
            }
            return "redirect:/invoices";
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/invoices/edit/" + id;
        }
    }

    @GetMapping("/detail/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("invoice", invoiceService.getInvoiceById(id));
        return "Invoice/detail";
    }


    @PostMapping("/{id}/issue")
    public String issue(@PathVariable Long id, RedirectAttributes ra) {
        try {
            invoiceService.issueInvoice(id);
            ra.addFlashAttribute("successMessage", "Hóa đơn đã được chốt và phát hành thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/invoices";
    }


    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        try {
            invoiceService.deleteInvoice(id);
            ra.addFlashAttribute("successMessage", "Đã xóa hóa đơn nháp thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/invoices";
    }
}