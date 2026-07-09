package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM InvoiceItem i " +
            "WHERE i.quotationDetail.id = :quotationDetailId " +
            "AND i.invoice.status <> 'CANCELLED'")
    Integer getTotalInvoicedQuantity(Long quotationDetailId);
}