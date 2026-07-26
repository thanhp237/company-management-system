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

    @Query("""
            select i
            from Invoice i
            where i.contract.customer.id = :customerId
            and (i.createdBy = :employeeId or i.updatedBy = :employeeId)
            order by i.createdAt desc
            """)
    java.util.List<Invoice> findByContractCustomerIdAndAccountantOrderByCreatedAtDesc(
            @Param("customerId") Long customerId,
            @Param("employeeId") Long employeeId);

    java.util.List<Invoice> findByContractCustomerIdAndContractSaleIdInOrderByCreatedAtDesc(
            Long customerId,
            java.util.List<Long> saleIds);

    long countByStatus(Invoice.InvoiceStatus status);

    @Query("""
            select count(i)
            from Invoice i
            where i.status = :status
            and (i.createdBy = :employeeId or i.updatedBy = :employeeId)
            """)
    long countByAccountantAndStatus(
            @Param("employeeId") Long employeeId,
            @Param("status") Invoice.InvoiceStatus status);

    @Query("select coalesce(sum(i.totalAmount), 0) from Invoice i where i.status = :status")
    BigDecimal sumTotalAmountByStatus(@Param("status") Invoice.InvoiceStatus status);

    @Query("""
            select coalesce(sum(i.totalAmount), 0)
            from Invoice i
            where i.status = :status
            and (i.createdBy = :employeeId or i.updatedBy = :employeeId)
            """)
    BigDecimal sumTotalAmountByAccountantAndStatus(
            @Param("employeeId") Long employeeId,
            @Param("status") Invoice.InvoiceStatus status);

    @Query("select coalesce(sum(i.paidAmount), 0) from Invoice i")
    BigDecimal sumPaidAmount();

    @Query("""
            select coalesce(sum(i.paidAmount), 0)
            from Invoice i
            where i.createdBy = :employeeId or i.updatedBy = :employeeId
            """)
    BigDecimal sumPaidAmountByAccountant(@Param("employeeId") Long employeeId);

    @Query("select coalesce(sum(i.outstandingAmount), 0) from Invoice i")
    BigDecimal sumOutstandingAmount();

    @Query("select coalesce(sum(i.paidAmount), 0) from Invoice i where i.createdAt >= :startDate and i.createdAt <= :endDate")
    BigDecimal sumPaidAmountBetween(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("select coalesce(sum(i.outstandingAmount), 0) from Invoice i where i.createdAt >= :startDate and i.createdAt <= :endDate")
    BigDecimal sumOutstandingAmountBetween(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("select coalesce(sum(i.paidAmount), 0) from Invoice i where i.contract.sale.id in :saleIds and i.createdAt >= :startDate and i.createdAt <= :endDate")
    BigDecimal sumPaidAmountBySaleIdInBetween(@Param("saleIds") java.util.List<Long> saleIds, @Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("select coalesce(sum(i.outstandingAmount), 0) from Invoice i where i.contract.sale.id in :saleIds and i.createdAt >= :startDate and i.createdAt <= :endDate")
    BigDecimal sumOutstandingAmountBySaleIdInBetween(@Param("saleIds") java.util.List<Long> saleIds, @Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    @Query("""
            select coalesce(sum(i.outstandingAmount), 0)
            from Invoice i
            where i.createdBy = :employeeId or i.updatedBy = :employeeId
            """)
    BigDecimal sumOutstandingAmountByAccountant(@Param("employeeId") Long employeeId);
}
