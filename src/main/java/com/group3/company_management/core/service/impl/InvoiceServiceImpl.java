package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.CreateInvoiceRequest;
import com.group3.company_management.core.dto.InvoiceItemFormRow;
import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Employee;
import com.group3.company_management.core.entity.Invoice;
import com.group3.company_management.core.entity.InvoiceItem;
import com.group3.company_management.core.entity.QuotationDetail;
import com.group3.company_management.core.repository.ContractRepository;
import com.group3.company_management.core.repository.InvoiceItemRepository;
import com.group3.company_management.core.repository.InvoiceRepository;
import com.group3.company_management.core.repository.QuotationDetailRepository;
import com.group3.company_management.core.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final ContractRepository contractRepository;
    private final QuotationDetailRepository quotationDetailRepository;

    @Override
    public Invoice createInvoice(Long contractId, CreateInvoiceRequest request) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng"));

        if (contract.getStatus() != Contract.ContractStatus.SIGNED) {
            throw new RuntimeException("Hợp đồng phải ở trạng thái SIGNED");
        }

        List<InvoiceItemFormRow> selectedItems = new ArrayList<>();
        if (request.getItems() != null) {
            for (InvoiceItemFormRow row : request.getItems()) {
                if (row.isSelected()) {
                    selectedItems.add(row);
                }
            }
        }

        if (selectedItems.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất một sản phẩm/dịch vụ để xuất hóa đơn");
        }

        Invoice.InvoiceStatus invoiceStatus = Invoice.InvoiceStatus.DRAFT;
        if ("ISSUED".equalsIgnoreCase(request.getStatus())) {
            invoiceStatus = Invoice.InvoiceStatus.ISSUED;
        }

        Long currentUserId = getCurrentUserId();

        Invoice invoice = Invoice.builder()
                .contract(contract)
                .invoiceCode(generateInvoiceCode())
                .dueDate(request.getDueDate())
                .note(request.getNote() != null ? request.getNote() : "")
                .status(invoiceStatus)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .outstandingAmount(BigDecimal.ZERO)
                .issuedAt(invoiceStatus == Invoice.InvoiceStatus.ISSUED ? LocalDateTime.now() : null)
                .createdBy(currentUserId)
                .updatedBy(currentUserId)
                .invoiceItems(new ArrayList<>())
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (InvoiceItemFormRow itemReq : selectedItems) {
            QuotationDetail qd = quotationDetailRepository.findById(itemReq.getQuotationDetailId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong báo giá"));

            if (!qd.getQuotation().getId().equals(contract.getQuotation().getId())) {
                throw new RuntimeException("Sản phẩm không thuộc hợp đồng này");
            }

            Integer alreadyExported = invoiceItemRepository.getTotalInvoicedQuantity(qd.getId());
            int remaining = qd.getQuantity() - (alreadyExported != null ? alreadyExported : 0);

            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new RuntimeException("Số lượng xuất phải lớn hơn 0");
            }

            if (itemReq.getQuantity() > remaining) {
                throw new RuntimeException("Số lượng xuất vượt quá số lượng còn lại trong hợp đồng cho sản phẩm: " + qd.getServiceName());
            }

            BigDecimal subtotal = qd.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .quotationDetail(qd)
                    .serviceName(qd.getServiceName())
                    .description(qd.getDescription())
                    .unitPrice(qd.getUnitPrice())
                    .quantity(itemReq.getQuantity())
                    .subtotal(subtotal)
                    .build();

            invoice.getInvoiceItems().add(item);
            totalAmount = totalAmount.add(subtotal);
        }

        invoice.setTotalAmount(totalAmount);
        invoice.setOutstandingAmount(totalAmount);

        return invoiceRepository.save(invoice);
    }

    @Override
    public Invoice issueInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new RuntimeException("Chỉ hóa đơn DRAFT mới được phát hành");
        }

        invoice.setStatus(Invoice.InvoiceStatus.ISSUED);
        invoice.setIssuedAt(LocalDateTime.now());
        invoice.setUpdatedBy(getCurrentUserId());
        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice getInvoiceById(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoicesFiltered(String search, String status, String sortBy, String order) {
        List<Invoice> list = new ArrayList<>(invoiceRepository.findAll());


        if (status != null && !status.trim().isEmpty()) {
            list.removeIf(inv -> !inv.getStatus().name().equalsIgnoreCase(status));
        }

        if (search != null && !search.trim().isEmpty()) {
            String lowerSearch = search.toLowerCase();
            list.removeIf(inv -> {
                boolean matchCode = inv.getInvoiceCode() != null && inv.getInvoiceCode().toLowerCase().contains(lowerSearch);

                String customerName = "";
                if (inv.getContract() != null) {
                    if (inv.getContract().getCustomer() != null) {
                        String fullName = inv.getContract().getCustomer().getFullName();
                        if (fullName == null) fullName = inv.getContract().getCustomer().getName();
                        if (fullName == null) fullName = inv.getContract().getCustomer().getCompanyName();
                        if (fullName != null) customerName = fullName.toLowerCase();
                    } else if (inv.getContract().getBuyerCompanyName() != null) {
                        customerName = inv.getContract().getBuyerCompanyName().toLowerCase();
                    }
                }
                boolean matchCustomer = customerName.contains(lowerSearch);
                return !matchCode && !matchCustomer;
            });
        }


        list.sort((a, b) -> {
            int cmp = 0;
            if ("totalAmount".equalsIgnoreCase(sortBy)) {
                BigDecimal amtA = a.getTotalAmount() != null ? a.getTotalAmount() : BigDecimal.ZERO;
                BigDecimal amtB = b.getTotalAmount() != null ? b.getTotalAmount() : BigDecimal.ZERO;
                cmp = amtA.compareTo(amtB);
            } else {
                LocalDateTime dateA = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                LocalDateTime dateB = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                cmp = dateA.compareTo(dateB);
            }
            return "asc".equalsIgnoreCase(order) ? cmp : -cmp;
        });

        return list;
    }

    @Override
    public void deleteInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new RuntimeException("Chỉ hóa đơn DRAFT mới được phép xóa");
        }
        invoiceRepository.delete(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public CreateInvoiceRequest prepareCreateRequest(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng"));

        if (contract.getStatus() != Contract.ContractStatus.SIGNED) {
            throw new RuntimeException("Hợp đồng phải ở trạng thái SIGNED");
        }

        List<QuotationDetail> quotationDetails = contract.getQuotation() != null
                ? contract.getQuotation().getDetails()
                : new ArrayList<>();

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        List<InvoiceItemFormRow> items = new ArrayList<>();

        for (QuotationDetail qd : quotationDetails) {
            Integer invoiced = invoiceItemRepository.getTotalInvoicedQuantity(qd.getId());
            int alreadyExported = invoiced != null ? invoiced : 0;
            int remaining = qd.getQuantity() - alreadyExported;

            items.add(InvoiceItemFormRow.builder()
                    .quotationDetailId(qd.getId())
                    .serviceName(qd.getServiceName() != null ? qd.getServiceName() : "Không có tên")
                    .description(qd.getDescription())
                    .contractQuantity(qd.getQuantity())
                    .invoicedQuantity(alreadyExported)
                    .remainingQuantity(Math.max(0, remaining))
                    .unitPrice(qd.getUnitPrice())
                    .quantity(Math.max(0, remaining))
                    .selected(false)
                    .build());
        }

        request.setItems(items);
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public CreateInvoiceRequest prepareEditRequest(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new RuntimeException("Chỉ hóa đơn DRAFT mới có thể chỉnh sửa");
        }

        Contract contract = invoice.getContract();
        List<QuotationDetail> quotationDetails = contract.getQuotation() != null
                ? contract.getQuotation().getDetails()
                : new ArrayList<>();


        Map<Long, Integer> currentInvoiceItems = new HashMap<>();
        for (InvoiceItem item : invoice.getInvoiceItems()) {
            currentInvoiceItems.put(item.getQuotationDetail().getId(), item.getQuantity());
        }

        CreateInvoiceRequest request = new CreateInvoiceRequest();
        request.setDueDate(invoice.getDueDate());
        request.setNote(invoice.getNote());

        List<InvoiceItemFormRow> items = new ArrayList<>();

        for (QuotationDetail qd : quotationDetails) {
            Integer totalInvoiced = invoiceItemRepository.getTotalInvoicedQuantity(qd.getId());
            int currentQtyInDraft = currentInvoiceItems.getOrDefault(qd.getId(), 0);

            // Tính số lượng còn lại loại trừ đi hóa đơn nháp hiện tại
            int invoicedByOthers = (totalInvoiced != null ? totalInvoiced : 0) - currentQtyInDraft;
            int remaining = qd.getQuantity() - invoicedByOthers;

            items.add(InvoiceItemFormRow.builder()
                    .quotationDetailId(qd.getId())
                    .serviceName(qd.getServiceName() != null ? qd.getServiceName() : "Không có tên")
                    .description(qd.getDescription())
                    .contractQuantity(qd.getQuantity())
                    .invoicedQuantity(invoicedByOthers)
                    .remainingQuantity(Math.max(0, remaining))
                    .unitPrice(qd.getUnitPrice())
                    .quantity(currentQtyInDraft > 0 ? currentQtyInDraft : Math.max(0, remaining))
                    .selected(currentQtyInDraft > 0)
                    .build());
        }

        request.setItems(items);
        return request;
    }

    @Override
    public Invoice updateInvoice(Long id, CreateInvoiceRequest request) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));

        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new RuntimeException("Chỉ hóa đơn DRAFT mới được chỉnh sửa");
        }

        List<InvoiceItemFormRow> selectedItems = new ArrayList<>();
        if (request.getItems() != null) {
            for (InvoiceItemFormRow row : request.getItems()) {
                if (row.isSelected()) {
                    selectedItems.add(row);
                }
            }
        }

        if (selectedItems.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất một sản phẩm/dịch vụ để xuất hóa đơn");
        }

        invoice.setDueDate(request.getDueDate());
        invoice.setNote(request.getNote() != null ? request.getNote() : "");
        invoice.setUpdatedBy(getCurrentUserId());

        if ("ISSUED".equalsIgnoreCase(request.getStatus())) {
            invoice.setStatus(Invoice.InvoiceStatus.ISSUED);
            invoice.setIssuedAt(LocalDateTime.now());
        }


        invoice.getInvoiceItems().clear();
        invoiceRepository.saveAndFlush(invoice);

        BigDecimal totalAmount = BigDecimal.ZERO;
        Contract contract = invoice.getContract();

        for (InvoiceItemFormRow itemReq : selectedItems) {
            QuotationDetail qd = quotationDetailRepository.findById(itemReq.getQuotationDetailId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong báo giá"));

            if (!qd.getQuotation().getId().equals(contract.getQuotation().getId())) {
                throw new RuntimeException("Sản phẩm không thuộc hợp đồng này");
            }

            Integer alreadyExported = invoiceItemRepository.getTotalInvoicedQuantity(qd.getId());
            int remaining = qd.getQuantity() - (alreadyExported != null ? alreadyExported : 0);

            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
                throw new RuntimeException("Số lượng xuất phải lớn hơn 0");
            }

            if (itemReq.getQuantity() > remaining) {
                throw new RuntimeException("Số lượng xuất vượt quá số lượng còn lại trong hợp đồng: " + qd.getServiceName());
            }

            BigDecimal subtotal = qd.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .quotationDetail(qd)
                    .serviceName(qd.getServiceName())
                    .description(qd.getDescription())
                    .unitPrice(qd.getUnitPrice())
                    .quantity(itemReq.getQuantity())
                    .subtotal(subtotal)
                    .build();

            invoice.getInvoiceItems().add(item);
            totalAmount = totalAmount.add(subtotal);
        }

        invoice.setTotalAmount(totalAmount);
        invoice.setOutstandingAmount(totalAmount);

        return invoiceRepository.save(invoice);
    }

    @Override
    public String generateNextInvoiceCode() {
        return generateInvoiceCode();
    }

    private String generateInvoiceCode() {
        Long count = invoiceRepository.count() + 1;
        java.time.LocalDate now = java.time.LocalDate.now();
        String yearMonth = String.format("%04d%02d", now.getYear(), now.getMonthValue());
        return String.format("INV%s%03d", yearMonth, count);
    }

    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();

                if (principal instanceof Employee) {
                    return ((Employee) principal).getId();
                } else if (principal instanceof Customer) {
                    return ((Customer) principal).getId();
                } else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    try {
                        java.lang.reflect.Method getIdMethod = principal.getClass().getMethod("getId");
                        return (Long) getIdMethod.invoke(principal);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}