package com.group3.company_management.core.service;

import com.group3.company_management.core.entity.Invoice;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.entity.Employee;
import com.group3.company_management.core.repository.InvoiceRepository;
import com.group3.company_management.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentReminderScheduler {

    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // Run every day at 8:00 AM
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendPaymentReminders() {
        log.info("⏰ Bắt đầu quét hóa đơn sắp đến hạn thanh toán...");
        
        LocalDate today = LocalDate.now();
        LocalDate tenDaysFromNow = today.plusDays(10);
        LocalDate threeDaysFromNow = today.plusDays(3);

        // Fetch all invoices
        List<Invoice> unpaidInvoices = invoiceRepository.findAll().stream()
                .filter(inv -> Invoice.InvoiceStatus.ISSUED.equals(inv.getStatus()))
                .toList();

        for (Invoice invoice : unpaidInvoices) {
            LocalDate dueDate = invoice.getDueDate();
            if (dueDate == null) continue;

            String customerEmail = null;
            if (invoice.getContract() != null && invoice.getContract().getCustomer() != null) {
                customerEmail = invoice.getContract().getCustomer().getEmail();
            }

            if (customerEmail == null || customerEmail.isBlank()) continue;

            if (dueDate.equals(tenDaysFromNow)) {
                sendReminder(customerEmail, invoice, 10);
            } else if (dueDate.equals(threeDaysFromNow)) {
                sendReminder(customerEmail, invoice, 3);
            } else if (dueDate.isBefore(today)) {
                sendReminder(customerEmail, invoice, -1); // Overdue
                sendOverdueToAccountants(invoice); // Gửi mail cho kế toán
            }
        }
        log.info("⏰ Hoàn tất quét hóa đơn.");
    }

    private void sendOverdueToAccountants(Invoice invoice) {
        try {
            String accountantEmail = null;
            
            // 1. Lấy theo người tạo hóa đơn (Kế toán phụ trách tạo hóa đơn này)
            if (invoice.getCreatedBy() != null) {
                Optional<User> creatorOpt = userRepository.findById(invoice.getCreatedBy());
                if (creatorOpt.isPresent() && creatorOpt.get().getEmail() != null && creatorOpt.get().isActive()) {
                    accountantEmail = creatorOpt.get().getEmail();
                }
            }
            
            // 2. Nếu không tìm thấy, lấy theo Kế toán phụ trách hợp đồng (adminOfficer - Kế toán phụ trách chính)
            if (accountantEmail == null && invoice.getContract() != null && invoice.getContract().getAdminOfficer() != null) {
                Employee adminOfficer = invoice.getContract().getAdminOfficer();
                if (adminOfficer.getUser() != null && adminOfficer.getUser().getEmail() != null && adminOfficer.getUser().isActive()) {
                    accountantEmail = adminOfficer.getUser().getEmail();
                }
            }

            if (accountantEmail != null && !accountantEmail.isBlank()) {
                String subject = "[CompanyMS] Cảnh báo quá hạn thanh toán - Hóa đơn " + invoice.getInvoiceCode();
                String content = String.format("""
                        Kính gửi bộ phận Kế toán (Nhân viên phụ trách),
                        
                        Hệ thống thông báo hóa đơn do bạn phụ trách đã quá hạn thanh toán:
                        - Mã hóa đơn: %s
                        - Hợp đồng: %s
                        - Khách hàng: %s
                        - Số tiền quá hạn: %,.0f VNĐ
                        - Hạn thanh toán ban đầu: %s
                        
                        Vui lòng liên hệ với khách hàng hoặc nhân viên kinh doanh phụ trách để đôn đốc thanh toán.
                        
                        Trân trọng,
                        Hệ thống quản trị CompanyMS.
                        """, 
                        invoice.getInvoiceCode(), 
                        invoice.getContract() != null ? invoice.getContract().getContractCode() : "N/A",
                        (invoice.getContract() != null && invoice.getContract().getCustomer() != null) ? invoice.getContract().getCustomer().getFullName() : "N/A",
                        invoice.getOutstandingAmount(), 
                        invoice.getDueDate());
                emailService.sendCustomEmail(accountantEmail, subject, content);
                log.info("✉️ Đã gửi cảnh báo hóa đơn quá hạn {} tới kế toán phụ trách {}", invoice.getInvoiceCode(), accountantEmail);
            }
        } catch (Exception e) {
            log.error("❌ Lỗi gửi email cảnh báo quá hạn cho kế toán phụ trách: {}", e.getMessage());
        }
    }

    private void sendReminder(String to, Invoice invoice, int days) {
        try {
            String subject;
            String content;
            if (days > 0) {
                subject = "[CompanyMS] Nhắc nhở thanh toán hóa đơn " + invoice.getInvoiceCode() + " (Còn " + days + " ngày)";
                content = String.format("""
                        Kính gửi Quý khách hàng,
                        
                        Chúng tôi xin nhắc nhở về hóa đơn %s sắp đến hạn thanh toán.
                        Số tiền cần thanh toán: %,.0f VNĐ.
                        Hạn thanh toán: %s (Còn %d ngày).
                        
                        Vui lòng truy cập cổng khách hàng tại hệ thống để tiến hành thanh toán qua VNPAY.
                        
                        Trân trọng cảm ơn,
                        Hệ thống quản trị CompanyMS.
                        """, invoice.getInvoiceCode(), invoice.getOutstandingAmount(), invoice.getDueDate(), days);
            } else {
                subject = "[CompanyMS] Cảnh báo hóa đơn " + invoice.getInvoiceCode() + " đã QUÁ HẠN thanh toán";
                content = String.format("""
                        Kính gửi Quý khách hàng,
                        
                        Hóa đơn %s của Quý khách đã quá hạn thanh toán.
                        Số tiền cần thanh toán: %,.0f VNĐ.
                        Hạn thanh toán ban đầu: %s.
                        
                        Vui lòng thanh toán sớm nhất có thể để tránh gián đoạn dịch vụ.
                        
                        Trân trọng cảm ơn,
                        Hệ thống quản trị CompanyMS.
                        """, invoice.getInvoiceCode(), invoice.getOutstandingAmount(), invoice.getDueDate());
            }
            emailService.sendCustomEmail(to, subject, content);
            log.info("✉️ Đã gửi nhắc nhở thanh toán cho: {} (Hóa đơn: {}, Còn lại: {} ngày)", to, invoice.getInvoiceCode(), days);

            // Also create customer portal notification
            if (invoice.getContract() != null && invoice.getContract().getCustomer() != null) {
                String title = days > 0 ? "Nhắc nhở thanh toán hóa đơn - " + invoice.getInvoiceCode() : "Hóa đơn đã quá hạn - " + invoice.getInvoiceCode();
                String msg = days > 0 ? "Hóa đơn số " + invoice.getInvoiceCode() + " cần thanh toán số tiền " + String.format("%,.0f", invoice.getOutstandingAmount()) + " VNĐ trước ngày " + invoice.getDueDate() + " (Còn " + days + " ngày)."
                                      : "Hóa đơn số " + invoice.getInvoiceCode() + " của bạn đã quá hạn thanh toán. Vui lòng thanh toán sớm nhất.";
                notificationService.createCustomerNotification(
                    invoice.getContract().getCustomer().getId(),
                    title,
                    msg
                );
            }
        } catch (Exception e) {
            log.error("❌ Lỗi gửi email nhắc nhở cho {}: {}", to, e.getMessage());
        }
    }
}
