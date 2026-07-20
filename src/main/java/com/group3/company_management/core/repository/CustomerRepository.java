// src/main/java/com/group3/company_management/core/repository/CustomerRepository.java

package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Contract;
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

    List<Customer> findByAssignedSalesIdInOrderByCreatedAtDesc(List<Long> assignedSalesIds);
    List<Customer> findByCustomerStatusAndAssignedSalesIdInOrderByCreatedAtDesc(String status, List<Long> assignedSalesIds);
    List<Customer> findByAssignedSalesIdIsNullOrderByCreatedAtDesc();
    List<Customer> findByCustomerStatusAndAssignedSalesIdIsNullOrderByCreatedAtDesc(String status);

    List<Customer> findByAssignedSalesIdOrderByCreatedAtDesc(Long assignedSalesId);
    List<Customer> findByCustomerStatusAndAssignedSalesIdOrderByCreatedAtDesc(String status, Long assignedSalesId);
    long countByAssignedSalesIdIn(List<Long> assignedSalesIds);

    @Query("""
            select distinct c
            from Customer c
            where (:status is null or c.customerStatus = :status)
            and exists (
                select i.id
                from Invoice i
                where i.contract.customer.id = c.id
                and (:employeeId is null or i.createdBy = :employeeId or i.updatedBy = :employeeId)
            )
            order by c.createdAt desc
            """)
    List<Customer> findCustomersWithInvoicesOrderByCreatedAtDesc(
            @Param("status") String status,
            @Param("employeeId") Long employeeId);

    @Query("""
            select count(i) > 0
            from Invoice i
            where i.contract.customer.id = :customerId
            and (:employeeId is null or i.createdBy = :employeeId or i.updatedBy = :employeeId)
            """)
    boolean existsInvoiceForCustomerId(
            @Param("customerId") Long customerId,
            @Param("employeeId") Long employeeId);

    @Query("""
            select distinct c
            from Customer c
            where (:status is null or c.customerStatus = :status)
            and exists (
                select co.id
                from Contract co
                where co.customer.id = c.id
                and co.adminOfficer.id = :adminOfficerId
            )
            order by c.createdAt desc
            """)
    List<Customer> findCustomersByAdminOfficerIdOrderByCreatedAtDesc(
            @Param("adminOfficerId") Long adminOfficerId,
            @Param("status") String status);

    @Query("""
            select distinct c
            from Customer c
            where (:status is null or c.customerStatus = :status)
            and exists (
                select co.id
                from Contract co
                where co.customer.id = c.id
                and (
                    co.adminOfficer.id = :adminOfficerId
                    or co.status in :pooledStatuses
                )
            )
            order by c.createdAt desc
            """)
    List<Customer> findCustomersByAdminOfficerScopeOrderByCreatedAtDesc(
            @Param("adminOfficerId") Long adminOfficerId,
            @Param("status") String status,
            @Param("pooledStatuses") List<Contract.ContractStatus> pooledStatuses);

    @Query("""
            select count(co) > 0
            from Contract co
            where co.customer.id = :customerId
            and co.adminOfficer.id = :adminOfficerId
            """)
    boolean existsContractForAdminOfficerAndCustomer(
            @Param("adminOfficerId") Long adminOfficerId,
            @Param("customerId") Long customerId);

    @Query("""
            select count(co) > 0
            from Contract co
            where co.customer.id = :customerId
            and (
                co.adminOfficer.id = :adminOfficerId
                or co.status in :pooledStatuses
            )
            """)
    boolean existsContractForAdminOfficerScopeAndCustomer(
            @Param("adminOfficerId") Long adminOfficerId,
            @Param("customerId") Long customerId,
            @Param("pooledStatuses") List<Contract.ContractStatus> pooledStatuses);

    long countByCustomerStatusIgnoreCase(String status);

    long countByAssignedSalesId(Long assignedSalesId);
    long countByAssignedSalesIdIsNotNull();

    long countByOwnerId(Long ownerId);
    
    /**
     * Find all customers (excluding soft deleted)
     */
    List<Customer> findAllByOrderByCreatedAtDesc();
    Customer findCustomerById(Long id);

}
