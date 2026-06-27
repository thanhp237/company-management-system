package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findByQuotationId(Long quotationId);

    List<Contract> findByStatus(Contract.ContractStatus status);

    List<Contract> findBySaleId(Long saleId);

    List<Contract> findByAdminOfficerId(Long adminOfficerId);
}