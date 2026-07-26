package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction,Long> {
    Optional<PaymentTransaction> findByTxnRef(String txnRef);
    Optional<PaymentTransaction> findTopByInvoiceIdAndStatusOrderByCreatedAtDesc(Long invoiceId, PaymentTransaction.Status status);
}