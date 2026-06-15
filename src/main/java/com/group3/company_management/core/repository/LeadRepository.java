package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends JpaRepository<Customer, Long>
{
}
