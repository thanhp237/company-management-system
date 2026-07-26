package com.group3.company_management.core.controller;

import com.group3.company_management.core.dto.CreateInvoiceRequest;
import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.entity.Invoice;
import com.group3.company_management.core.repository.ContractRepository;
import com.group3.company_management.core.service.CustomerReportScopeService;
import com.group3.company_management.core.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import com.group3.company_management.core.entity.PaymentSchedule;
import java.math.BigDecimal;
import java.util.List;
@Controller
@RequestMapping("/invoices")
@RequiredArgsConstructor

@PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN', 'DIRECTOR','CUSTOMER', 'MANAGER', 'SALES_MANAGER', 'SALES', 'ADMIN_OFFICER', 'ADMINOFFICER')")


public class InvoiceController {

    private final InvoiceService invoiceService;
    private final ContractRepository contractRepository;
    private final com.group3.company_management.core.repository.UserRepository userRepository;
    private final CustomerReportScopeService customerReportScopeService;
    private final com.group3.company_management.core.service.VnPayService vnPayService;


    @GetMapping
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
                       @RequestParam(required = false, defaultValue = "desc") String order,
                       Model model) {

        int syncedDraftCount = invoiceService.syncDraftInvoicesForSignedContracts();
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
        model.addAttribute("syncedDraftCount", syncedDraftCount);

        return "Invoice/list";
    }

    @GetMapping("/create/{contractId}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    public String createForm(@PathVariable Long contractId,
                             @RequestParam(required = false) Long scheduleId, Model model) {
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

        CreateInvoiceRequest form = invoiceService.prepareCreateRequest(contractId);
        if (scheduleId != null) {
            form.setPaymentScheduleId(scheduleId);
            // Tự động tìm đợt thanh toán trong hợp đồng để gán đúng Hạn thanh toán và nội dung ghi chú
            if (contract.getPaymentSchedules() != null) {
                for (PaymentSchedule s : contract.getPaymentSchedules()) {
                    if (s.getId().equals(scheduleId)) {
                        form.setDueDate(s.getDueDate());
                        form.setNote(s.getDescription());
                        break;
                    }
                }
            }
        }
        model.addAttribute("invoiceForm", form);

        // --- TỰ ĐỘNG TÍNH TOÁN TIẾN ĐỘ CHO GIAO DIỆN TẠO MỚI ---
        BigDecimal totalContractAmount = contract.getFinalAmount();
        BigDecimal paidPreviously = BigDecimal.ZERO;
        BigDecimal remainingAmount = totalContractAmount;
        int currentNo = 1;
        int totalInstallments = 1;
        double percent = 100.0;
        BigDecimal installmentAmount = totalContractAmount;

        List<PaymentSchedule> allSchedules = contract.getPaymentSchedules();
        totalInstallments = allSchedules.size();

        Long resolvedScheduleId = form.getPaymentScheduleId();
        if (resolvedScheduleId != null) {
            for (PaymentSchedule s : allSchedules) {
                if (s.getId().equals(resolvedScheduleId)) {
                    currentNo = s.getInstallmentNo();
                    installmentAmount = s.getAmount();
                }
            }
            for (PaymentSchedule s : allSchedules) {
                if (s.getInstallmentNo() < currentNo) {
                    paidPreviously = paidPreviously.add(s.getAmount());
                }
            }
            if (totalContractAmount.compareTo(BigDecimal.ZERO) > 0) {
                percent = (installmentAmount.doubleValue() / totalContractAmount.doubleValue()) * 100;
            }
            remainingAmount = totalContractAmount.subtract(paidPreviously).subtract(installmentAmount);
        } else {
            remainingAmount = BigDecimal.ZERO;
        }

        model.addAttribute("currentNo", currentNo);
        model.addAttribute("totalInstallments", totalInstallments);
        model.addAttribute("percent", Math.round(percent));
        model.addAttribute("installmentAmount", installmentAmount);
        model.addAttribute("paidPreviously", paidPreviously);
        model.addAttribute("remainingAmount", remainingAmount);

        return "Invoice/invoice-form";
    }

    @PostMapping("/create/{contractId}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
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

        // --- TỰ ĐỘNG TÍNH TOÁN TIẾN ĐỘ CHO GIAO DIỆN CHỈNH SỬA ---
        BigDecimal totalContractAmount = contract.getFinalAmount();
        BigDecimal paidPreviously = BigDecimal.ZERO;
        BigDecimal remainingAmount = totalContractAmount;
        int currentNo = 1;
        int totalInstallments = 1;
        double percent = 100.0;
        BigDecimal installmentAmount = totalContractAmount;

        if (invoice.getPaymentSchedule() != null) {
            PaymentSchedule currentSchedule = invoice.getPaymentSchedule();
            currentNo = currentSchedule.getInstallmentNo();
            installmentAmount = currentSchedule.getAmount();
            List<PaymentSchedule> allSchedules = contract.getPaymentSchedules();
            totalInstallments = allSchedules.size();

            for (PaymentSchedule s : allSchedules) {
                if (s.getInstallmentNo() < currentNo) {
                    paidPreviously = paidPreviously.add(s.getAmount());
                }
            }
            if (totalContractAmount.compareTo(BigDecimal.ZERO) > 0) {
                percent = (installmentAmount.doubleValue() / totalContractAmount.doubleValue()) * 100;
            }
            remainingAmount = totalContractAmount.subtract(paidPreviously).subtract(installmentAmount);
        } else {
            remainingAmount = BigDecimal.ZERO;
        }

        model.addAttribute("currentNo", currentNo);
        model.addAttribute("totalInstallments", totalInstallments);
        model.addAttribute("percent", Math.round(percent));
        model.addAttribute("installmentAmount", installmentAmount);
        model.addAttribute("paidPreviously", paidPreviously);
        model.addAttribute("remainingAmount", remainingAmount);

        return "Invoice/invoice-form";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
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
    public String detail(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (!customerReportScopeService.canViewInvoice(id, authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xem hóa đơn này.");
            return "redirect:/invoices";
        }
        Invoice invoice = invoiceService.getInvoiceById(id);
        
        if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"))) {
            com.group3.company_management.core.entity.Customer customer = (com.group3.company_management.core.entity.Customer) authentication.getPrincipal();
            if (invoice.getContract().getCustomer() == null || !customer.getId().equals(invoice.getContract().getCustomer().getId())) {
                throw new RuntimeException("Bạn không có quyền xem hóa đơn này.");
            }
            if (invoice.getStatus() == Invoice.InvoiceStatus.DRAFT) {
                throw new RuntimeException("Hóa đơn chưa sẵn sàng (Bản nháp).");
            }
        }
        model.addAttribute("invoice", invoice);
        BigDecimal totalContractAmount = invoice.getContract().getFinalAmount();
        BigDecimal paidPreviously = BigDecimal.ZERO;
        BigDecimal remainingAmount = totalContractAmount;
        int currentNo = 1;
        int totalInstallments = 1;
        double percent = 100.0;

        if (invoice.getPaymentSchedule() != null) {
            PaymentSchedule currentSchedule = invoice.getPaymentSchedule();
            currentNo = currentSchedule.getInstallmentNo();
            List<PaymentSchedule> allSchedules = invoice.getContract().getPaymentSchedules();
            totalInstallments = allSchedules.size();
            for (PaymentSchedule s : allSchedules) {
                if (s.getInstallmentNo() < currentNo) {
                    paidPreviously = paidPreviously.add(s.getAmount());
                }
            }
            if (totalContractAmount.compareTo(BigDecimal.ZERO) > 0) {
                percent = (currentSchedule.getAmount().doubleValue() / totalContractAmount.doubleValue()) * 100;
            }
            remainingAmount = totalContractAmount.subtract(paidPreviously).subtract(currentSchedule.getAmount());
        } else {
            remainingAmount = BigDecimal.ZERO;
        }

        // 2. Đẩy các thông số tính toán này sang giao diện HTML hiển thị
        model.addAttribute("currentNo", currentNo);
        model.addAttribute("totalInstallments", totalInstallments);
        model.addAttribute("percent", Math.round(percent));
        model.addAttribute("paidPreviously", paidPreviously);
        model.addAttribute("remainingAmount", remainingAmount);

        return "Invoice/detail";
    }



    @PostMapping("/{id}/confirm-payment")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    public String confirmPayment(@PathVariable Long id, RedirectAttributes ra) {
        try {
            vnPayService.confirmReviewedPayment(id);
            ra.addFlashAttribute("successMessage", "Đã xác nhận hoàn tất thanh toán hóa đơn.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/invoices/detail/" + id;
    }
    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
    public String issue(@PathVariable Long id, RedirectAttributes ra) {
        try {
            invoiceService.issueInvoice(id);
            ra.addFlashAttribute("successMessage", "Hóa đơn đã được chốt và phát hành thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/invoices";
    }
    @GetMapping("/print/{id}")
    public String print(@PathVariable Long id,
                        Authentication authentication,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        if (!customerReportScopeService.canViewInvoice(id, authentication)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền in hóa đơn này.");
            return "redirect:/invoices";
        }
        Invoice invoice = invoiceService.getInvoiceById(id);
        model.addAttribute("invoice", invoice);

        // Tính toán thông tin tiến độ đóng tiền cho bản in PDF
        BigDecimal totalContractAmount = invoice.getContract().getFinalAmount();
        BigDecimal paidPreviously = BigDecimal.ZERO;
        BigDecimal remainingAmount = totalContractAmount;
        int currentNo = 1;
        int totalInstallments = 1;
        double percent = 100.0;

        if (invoice.getPaymentSchedule() != null) {
            PaymentSchedule currentSchedule = invoice.getPaymentSchedule();
            currentNo = currentSchedule.getInstallmentNo();
            List<PaymentSchedule> allSchedules = invoice.getContract().getPaymentSchedules();
            totalInstallments = allSchedules.size();

            for (PaymentSchedule s : allSchedules) {
                if (s.getInstallmentNo() < currentNo) {
                    paidPreviously = paidPreviously.add(s.getAmount());
                }
            }

            if (totalContractAmount.compareTo(BigDecimal.ZERO) > 0) {
                percent = (currentSchedule.getAmount().doubleValue() / totalContractAmount.doubleValue()) * 100;
            }
            remainingAmount = totalContractAmount.subtract(paidPreviously).subtract(currentSchedule.getAmount());
        } else {
            remainingAmount = BigDecimal.ZERO;
        }

        model.addAttribute("currentNo", currentNo);
        model.addAttribute("totalInstallments", totalInstallments);
        model.addAttribute("percent", Math.round(percent));
        model.addAttribute("installmentAmount", invoice.getTotalAmount());
        model.addAttribute("paidPreviously", paidPreviously);
        model.addAttribute("remainingAmount", remainingAmount);

        // Lấy tên kế toán phụ trách (ưu tiên người tạo hóa đơn, tiếp theo là Kế toán/Hành chính phụ trách hợp đồng)
        String accountantName = null;
        if (invoice.getCreatedBy() != null) {
            var creatorOpt = userRepository.findById(invoice.getCreatedBy());
            if (creatorOpt.isPresent()) {
                accountantName = creatorOpt.get().getFullName();
            }
        }
        if ((accountantName == null || accountantName.isBlank()) 
                && invoice.getContract() != null && invoice.getContract().getAdminOfficer() != null) {
            com.group3.company_management.core.entity.Employee adminOfficer = invoice.getContract().getAdminOfficer();
            if (adminOfficer.getUser() != null) {
                accountantName = adminOfficer.getUser().getFullName();
            }
        }
        if (accountantName == null || accountantName.isBlank()) {
            String approver = invoice.getContract().getQuotation() != null ? invoice.getContract().getQuotation().getApprovedBy() : "admin";
            if (approver != null) {
                var approverUserOpt = userRepository.findByUsername(approver);
                accountantName = approverUserOpt.map(com.group3.company_management.core.entity.User::getFullName).orElse(approver);
            } else {
                accountantName = "Người phụ trách";
            }
        }
        model.addAttribute("approverName", accountantName);

        return "Invoice/print";
    }


    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyRole('ACCOUNTANT', 'ADMIN')")
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
