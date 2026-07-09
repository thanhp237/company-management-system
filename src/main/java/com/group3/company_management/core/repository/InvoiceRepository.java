package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    boolean existsByPaymentScheduleId(Long paymentScheduleId);
    java.util.Optional<Invoice> findByPaymentScheduleId(Long paymentScheduleId);
    java.util.List<Invoice> findByContractCustomerIdOrderByCreatedAtDesc(Long customerId);
}
