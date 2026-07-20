package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Contract;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long>, JpaSpecificationExecutor<Contract> {

    Optional<Contract> findByQuotationId(Long quotationId);

    List<Contract> findByStatus(Contract.ContractStatus status);

    long countByStatus(Contract.ContractStatus status);

    List<Contract> findBySaleId(Long saleId);

    List<Contract> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<Contract> findByCustomerIdAndSaleIdOrderByCreatedAtDesc(Long customerId, Long saleId);

    List<Contract> findByCustomerIdAndSaleIdInOrderByCreatedAtDesc(Long customerId, List<Long> saleIds);

    List<Contract> findByCustomerIdAndAdminOfficerIdOrderByCreatedAtDesc(Long customerId, Long adminOfficerId);

    @Query("""
            select distinct c
            from Contract c
            join Invoice i on i.contract = c
            where c.customer.id = :customerId
            and (i.createdBy = :employeeId or i.updatedBy = :employeeId)
            order by c.createdAt desc
            """)
    List<Contract> findByCustomerIdAndHasScopedInvoiceOrderByCreatedAtDesc(
            @Param("customerId") Long customerId,
            @Param("employeeId") Long employeeId);

    long countByCustomerId(Long customerId);

    long countByCustomerIdAndStatus(Long customerId, Contract.ContractStatus status);

    List<Contract> findByAdminOfficerId(Long adminOfficerId);
    long countBySaleId(Long saleId);

    long countBySaleIdAndStatus(Long saleId, Contract.ContractStatus status);

    long countByAdminOfficerId(Long adminOfficerId);

    long countByAdminOfficerIdAndStatus(Long adminOfficerId, Contract.ContractStatus status);

    @Query("select coalesce(sum(c.finalAmount), 0) from Contract c where c.status = :status")
    BigDecimal sumFinalAmountByStatus(@Param("status") Contract.ContractStatus status);

    @Query("select coalesce(sum(c.finalAmount), 0) from Contract c where c.sale.id = :saleId and c.status = :status")
    BigDecimal sumFinalAmountBySaleIdAndStatus(@Param("saleId") Long saleId, @Param("status") Contract.ContractStatus status);

    @Query("""
            select coalesce(sum(c.finalAmount), 0)
            from Contract c
            where c.sale.id in :saleIds
            and c.status = :status
            """)
    BigDecimal sumFinalAmountBySaleIdInAndStatus(@Param("saleIds") List<Long> saleIds, @Param("status") Contract.ContractStatus status);

    @Query("""
            select count(c) > 0
            from Contract c
            where c.id <> :contractId
            and c.customer.id <> :customerId
            and c.buyerBankAccount is not null
            and replace(replace(lower(c.buyerBankAccount), ' ', ''), '-', '') = :normalizedBankAccount
            """)
    boolean existsDuplicateBuyerBankAccount(
            @Param("contractId") Long contractId,
            @Param("customerId") Long customerId,
            @Param("normalizedBankAccount") String normalizedBankAccount);

    long countBySaleIdIn(List<Long> saleIds);

    long countBySaleIdInAndStatus(List<Long> saleIds, Contract.ContractStatus status);
}
