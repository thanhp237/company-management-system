package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    Optional<Quotation> findFirstByOpportunityIdOrderByCreatedAtDesc(Long opportunityId);
}
