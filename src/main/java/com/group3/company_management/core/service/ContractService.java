package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.ContractRuleRequest;
import com.group3.company_management.core.dto.ContractResponse;
import com.group3.company_management.core.dto.ContractStatisticsResponse;
import com.group3.company_management.core.entity.Contract;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ContractService {

    Long createFromQuotation(Long quotationId, String username);

    ContractResponse getContractDetail(Long contractId);

    List<ContractResponse> getMyContracts(String username);

    List<ContractResponse> getPendingAdminContracts();

    List<ContractResponse> getCustomerContracts(Long customerId);

    ContractResponse getCustomerContractDetail(Long contractId, Long customerId);

    void submitToAdmin(Long contractId, String username);

    void updateDraftContractInfo(Long contractId, ContractRuleRequest request, String username);

    void updateContractRules(Long contractId, ContractRuleRequest request, String adminUsername);

    void sendToCustomer(Long contractId, String username);

    void customerSignContractByCustomer(Long contractId, Long customerId);
    void customerRequestRevision(Long contractId, Long customerId, String reason);

    void cancelContract(Long contractId, String username);
    Page<ContractResponse> searchContracts(
            String username,
            String keyword,
            Contract.ContractStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    ContractStatisticsResponse getContractStatistics(String username);

    void deleteContract(Long contractId, String username);
}
