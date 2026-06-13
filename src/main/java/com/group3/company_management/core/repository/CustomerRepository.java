package com.group3.company_management.core.repository;

// src/main/java/com/group3/company_management/core/repository/CustomerRepository.java

import com.group3.company_management.core.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Customer repository - same pattern as UserRepository
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    /**
     * Find customer by phone
     */
    Optional<Customer> findByPhone(String phone);
    
    /**
     * Find all active customers
     */
    List<Customer> findByCustomerStatusOrderByCreatedAtDesc(String status);
    
    /**
     * Find all customers (excluding soft deleted)
     */
    List<Customer> findAllByOrderByCreatedAtDesc();
}