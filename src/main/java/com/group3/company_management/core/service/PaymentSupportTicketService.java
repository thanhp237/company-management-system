package com.group3.company_management.core.service;

import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Invoice;
import com.group3.company_management.core.entity.PaymentSupportTicket;
import com.group3.company_management.core.repository.InvoiceRepository;
import com.group3.company_management.core.repository.PaymentSupportTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentSupportTicketService {
    private static final Path SUPPORT_UPLOAD_DIR = Paths.get("uploads", "support");
    private static final String SUPPORT_UPLOAD_URL_PREFIX = "/uploads/support/";
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final PaymentSupportTicketRepository ticketRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public List<PaymentSupportTicket> getTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Invoice getCustomerInvoice(Long invoiceId, Long customerId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hóa đơn."));
        if (invoice.getContract() == null
                || invoice.getContract().getCustomer() == null
                || !customerId.equals(invoice.getContract().getCustomer().getId())) {
            throw new IllegalArgumentException("Bạn không có quyền gửi hỗ trợ cho hóa đơn này.");
        }
        return invoice;
    }

    public void createTicket(Long invoiceId, Customer customer, String title, String content, MultipartFile imageFile) {
        Invoice invoice = getCustomerInvoice(invoiceId, customer.getId());
        String normalizedTitle = normalizeRequired(title, "Vui lòng nhập tiêu đề hỗ trợ.");
        String normalizedContent = normalizeRequired(content, "Vui lòng nhập nội dung hỗ trợ.");

        ticketRepository.save(PaymentSupportTicket.builder()
                .customer(customer)
                .invoice(invoice)
                .title(normalizedTitle)
                .content(normalizedContent)
                .imageUrl(storeSupportImage(imageFile))
                .build());
    }

    public void resolveTicket(Long ticketId) {
        PaymentSupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hỗ trợ."));
        ticket.setStatus(PaymentSupportTicket.Status.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
    }

    private String storeSupportImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }

        validateImageFile(imageFile);
        String extension = getFileExtension(imageFile.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try {
            Files.createDirectories(SUPPORT_UPLOAD_DIR);
            Path destination = SUPPORT_UPLOAD_DIR.resolve(filename).toAbsolutePath().normalize();
            Path uploadRoot = SUPPORT_UPLOAD_DIR.toAbsolutePath().normalize();
            if (!destination.startsWith(uploadRoot)) {
                throw new IllegalArgumentException("Tên file ảnh không hợp lệ.");
            }
            Files.copy(imageFile.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return SUPPORT_UPLOAD_URL_PREFIX + filename;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể lưu ảnh minh chứng.");
        }
    }

    private void validateImageFile(MultipartFile imageFile) {
        if (imageFile.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Ảnh minh chứng không được vượt quá 5MB.");
        }

        String extension = getFileExtension(imageFile.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Ảnh minh chứng chỉ hỗ trợ JPG hoặc PNG.");
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("File tải lên phải là ảnh JPG hoặc PNG.");
        }
    }

    private String getFileExtension(String originalFilename) {
        String filename = normalizeOptional(originalFilename);
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("Ảnh minh chứng phải có phần mở rộng file.");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
}
