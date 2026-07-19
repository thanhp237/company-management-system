package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.PaymentSupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentSupportTicketRepository extends JpaRepository<PaymentSupportTicket, Long> {
    List<PaymentSupportTicket> findAllByOrderByCreatedAtDesc();
}
