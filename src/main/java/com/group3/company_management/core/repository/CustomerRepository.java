// src/main/java/com/group3/company_management/core/repository/CustomerRepository.java

package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Customer repository - UPDATED with authentication queries
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    
    // ========== AUTHENTICATION QUERIES (NEW) ==========
    
    /**
     * Find customer by email for login (not deleted)
     */
    @Query("SELECT c FROM Customer c WHERE c.email = :email AND c.isDeleted = false")
    Optional<Customer> findByEmailAndNotDeleted(@Param("email") String email);
    
    /**
     * Find customer by phone
     */
    Optional<Customer> findByPhone(String phone);
    
    // ========== EXISTING QUERIES ==========
    
    /**
     * Find all active customers
     */
    List<Customer> findByCustomerStatusOrderByCreatedAtDesc(String status);

    long countByCustomerStatusIgnoreCase(String status);

    long countByAssignedSalesId(Long assignedSalesId);

    long countByOwnerId(Long ownerId);
    
    /**
     * Find all customers (excluding soft deleted)
     */
    List<Customer> findAllByOrderByCreatedAtDesc();
    Customer findCustomerById(Long id);

}
