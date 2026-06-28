package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long>, JpaSpecificationExecutor<Contract> {

    Optional<Contract> findByQuotationId(Long quotationId);

    List<Contract> findByStatus(Contract.ContractStatus status);

    List<Contract> findBySaleId(Long saleId);

    List<Contract> findByAdminOfficerId(Long adminOfficerId);
    long countBySaleId(Long saleId);

    long countBySaleIdAndStatus(Long saleId, Contract.ContractStatus status);

    long countByAdminOfficerId(Long adminOfficerId);

    long countByAdminOfficerIdAndStatus(Long adminOfficerId, Contract.ContractStatus status);
}