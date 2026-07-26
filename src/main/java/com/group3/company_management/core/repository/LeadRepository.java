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

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Customer c ORDER BY CASE WHEN c.assignedSalesId IS NULL THEN 0 ELSE 1 END ASC, c.createdAt DESC, c.id DESC")
    Page<Customer> findAllOrderUnassignedFirst(Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Customer c WHERE c.assignedSalesId IS NULL OR c.assignedSalesId IN :saleIds ORDER BY CASE WHEN c.assignedSalesId IS NULL THEN 0 ELSE 1 END ASC, c.createdAt DESC, c.id DESC")
    Page<Customer> findUnassignedOrAssignedSalesIdInOrderUnassignedFirst(@org.springframework.data.repository.query.Param("saleIds") java.util.List<Long> saleIds, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT c FROM Customer c WHERE c.assignedSalesId IN :saleIds ORDER BY c.createdAt DESC, c.id DESC")
    Page<Customer> findByAssignedSalesIdInOrderDesc(@org.springframework.data.repository.query.Param("saleIds") java.util.List<Long> saleIds, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(c) FROM Customer c WHERE c.assignedSalesId IS NULL OR c.assignedSalesId IN :saleIds")
    long countUnassignedOrAssignedSalesIdIn(@org.springframework.data.repository.query.Param("saleIds") java.util.List<Long> saleIds);

    long countByAssignedSalesIdIn(java.util.List<Long> saleIds);
}
