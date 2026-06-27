package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Opportunity;
import com.group3.company_management.core.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Opportunity> findByCustomerId(Long customerId);


}
