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
