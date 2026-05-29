package com.group3.company_management.core.repository;

// src/main/java/com/group3/company_management/customer/repository/CustomerRepository.java
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.group3.company_management.core.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    /**
     * Find customer by phone (excluding soft-deleted)
     */
    Optional<Customer> findByPhone(String phone);
    
    /**
     * Find by phone excluding a specific customer (for update validation)
     */
    @Query("SELECT c FROM Customer c WHERE c.phone = :phone AND c.id != :customerId")
    Optional<Customer> findByPhoneExcluding(@Param("phone") String phone, @Param("customerId") Long customerId);
    
    /**
     * Search customers by name or phone with pagination
     */
    @Query("""
        SELECT c FROM Customer c 
        WHERE (LOWER(c.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
               OR c.phone LIKE CONCAT('%', :searchTerm, '%'))
        """)
    Page<Customer> searchByNameOrPhone(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    /**
     * Find customers by status with pagination
     */
    Page<Customer> findByCustomerStatus(String status, Pageable pageable);
    
    /**
     * Search by name/phone and filter by status
     */
    @Query("""
        SELECT c FROM Customer c 
        WHERE (LOWER(c.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) 
               OR c.phone LIKE CONCAT('%', :searchTerm, '%'))
        AND c.customerStatus = :status
        """)
    Page<Customer> searchByNameOrPhoneAndStatus(
        @Param("searchTerm") String searchTerm,
        @Param("status") String status,
        Pageable pageable
    );
    
    /**
     * Find customers by assigned sales person
     */
    Page<Customer> findByAssignedSalesId(Long assignedSalesId, Pageable pageable);
}