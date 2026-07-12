package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<Customer, Long>
{
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    boolean  existsByTaxCode(String taxcode);
    long countByAssignedSalesIdIsNull();
    long countByAssignedSalesIdIsNotNull();
    Page<Customer> findByAssignedSalesIdIsNull(Pageable pageable);

    Page<Customer> findByAssignedSalesIdIsNotNull(Pageable pageable);

}
