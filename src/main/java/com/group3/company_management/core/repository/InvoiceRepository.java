package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    boolean existsByPaymentScheduleId(Long paymentScheduleId);
    java.util.Optional<Invoice> findByPaymentScheduleId(Long paymentScheduleId);
    java.util.List<Invoice> findByContractCustomerIdOrderByCreatedAtDesc(Long customerId);

    long countByStatus(Invoice.InvoiceStatus status);

    @Query("select coalesce(sum(i.totalAmount), 0) from Invoice i where i.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") Invoice.InvoiceStatus status);

    @Query("select coalesce(sum(i.paidAmount), 0) from Invoice i")
    BigDecimal sumPaidAmount();

    @Query("select coalesce(sum(i.outstandingAmount), 0) from Invoice i")
    BigDecimal sumOutstandingAmount();
}
