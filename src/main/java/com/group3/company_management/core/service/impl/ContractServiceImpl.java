package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.ContractItemResponse;
import com.group3.company_management.core.dto.ContractRuleRequest;
import com.group3.company_management.core.dto.ContractResponse;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.entity.Employee;
import com.group3.company_management.core.entity.Product;
import com.group3.company_management.core.entity.Quotation;
import com.group3.company_management.core.entity.QuotationDetail;
import com.group3.company_management.core.repository.ContractRepository;
import com.group3.company_management.core.repository.EmployeeRepository;
import com.group3.company_management.core.repository.QuotationRepository;
import com.group3.company_management.core.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.group3.company_management.core.dto.ContractStatisticsResponse;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements ContractService {

    private static final String APPROVED_QUOTATION_STATUS = "APPROVED";
    private static final String ADMIN_OFFICER_TYPE = "ADMIN_OFFICER";
    private static final String ADMIN_OFFICER_TYPE_ALIAS = "ADMINOFFICER";
    private static final String ADMIN_TYPE = "ADMIN";

    private final QuotationRepository quotationRepository;
    private final ContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional
    public Long createFromQuotation(Long quotationId, String username) {
        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new RuntimeException("Quotation not found"));

        if (!APPROVED_QUOTATION_STATUS.equalsIgnoreCase(quotation.getStatus())) {
            throw new RuntimeException("Only approved quotation can create contract");
        }

        contractRepository.findByQuotationId(quotationId)
                .ifPresent(existingContract -> {
                    throw new RuntimeException("Contract already exists for this quotation");
                });

        Employee sale = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Sale employee not found"));

        Contract contract = new Contract();
        contract.setQuotation(quotation);
        contract.setCustomer(quotation.getCustomer());
        contract.setContractCode("CT-" + System.currentTimeMillis());
        contract.setStatus(Contract.ContractStatus.DRAFT);
        contract.setContractAmount(defaultMoney(quotation.getSubTotal()));
        contract.setDiscountAmount(defaultMoney(quotation.getDiscountAmount()));
        contract.setFinalAmount(defaultMoney(quotation.getFinalAmount()));
        contract.setSale(sale);
        contract.setAssignedEmployee(sale);

        Contract savedContract = contractRepository.save(contract);

        return savedContract.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getContractDetail(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        return mapToResponse(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> getMyContracts(String username) {
        Employee employee = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        List<Contract> contracts = canReviewContract(employee)
                ? contractRepository.findByAdminOfficerId(employee.getId())
                : contractRepository.findBySaleId(employee.getId());

        return contracts
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> getPendingAdminContracts() {
        return contractRepository.findByStatus(Contract.ContractStatus.PENDING_ADMIN_OFFICER)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void submitToAdmin(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if (contract.getStatus() != Contract.ContractStatus.DRAFT) {
            throw new RuntimeException("Only draft contract can be submitted to admin officer");
        }

        contract.setStatus(Contract.ContractStatus.PENDING_ADMIN_OFFICER);
        contractRepository.save(contract);
    }

    @Override
    @Transactional
    public void updateContractRules(Long contractId, ContractRuleRequest request, String adminUsername) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if (contract.getStatus() != Contract.ContractStatus.PENDING_ADMIN_OFFICER) {
            throw new RuntimeException("Only contract pending admin officer can update contract rules");
        }

        Employee adminOfficer = employeeRepository.findByUser_Username(adminUsername)
                .orElseThrow(() -> new RuntimeException("Admin officer employee not found"));

        if (!canReviewContract(adminOfficer)) {
            throw new RuntimeException("Only admin officer can update contract rules");
        }

        contract.setAdminOfficer(adminOfficer);
        contract.setAssignedEmployee(adminOfficer);
        contract.setContractStartDate(request.getContractStartDate());
        contract.setContractEndDate(request.getContractEndDate());
        contract.setPaymentTerms(request.getPaymentTerms());
        contract.setDeliveryTerms(request.getDeliveryTerms());
        contract.setLegalTerms(request.getLegalTerms());
        contract.setAdminNote(request.getAdminNote());
        contract.setSigningDate(request.getSigningDate());
        contract.setSigningPlace(request.getSigningPlace());
        contract.setSellerCompanyName(request.getSellerCompanyName());
        contract.setSellerTaxCode(request.getSellerTaxCode());
        contract.setSellerAddress(request.getSellerAddress());
        contract.setSellerPhone(request.getSellerPhone());
        contract.setSellerFax(request.getSellerFax());
        contract.setSellerBankAccount(request.getSellerBankAccount());
        contract.setSellerBankName(request.getSellerBankName());
        contract.setSellerRepresentativeName(request.getSellerRepresentativeName());
        contract.setSellerRepresentativeTitle(request.getSellerRepresentativeTitle());
        contract.setSellerIdentityNumber(request.getSellerIdentityNumber());
        contract.setSellerIdentityIssuedPlace(request.getSellerIdentityIssuedPlace());
        contract.setSellerIdentityIssuedDate(request.getSellerIdentityIssuedDate());
        contract.setSellerAuthorizationInfo(request.getSellerAuthorizationInfo());
        contract.setBuyerCompanyName(request.getBuyerCompanyName());
        contract.setBuyerTaxCode(request.getBuyerTaxCode());
        contract.setBuyerAddress(request.getBuyerAddress());
        contract.setBuyerPhone(request.getBuyerPhone());
        contract.setBuyerFax(request.getBuyerFax());
        contract.setBuyerBankAccount(request.getBuyerBankAccount());
        contract.setBuyerBankName(request.getBuyerBankName());
        contract.setBuyerRepresentativeName(request.getBuyerRepresentativeName());
        contract.setBuyerRepresentativeTitle(request.getBuyerRepresentativeTitle());
        contract.setBuyerIdentityNumber(request.getBuyerIdentityNumber());
        contract.setBuyerIdentityIssuedPlace(request.getBuyerIdentityIssuedPlace());
        contract.setBuyerIdentityIssuedDate(request.getBuyerIdentityIssuedDate());
        contract.setBuyerAuthorizationInfo(request.getBuyerAuthorizationInfo());
        contract.setAmountInWords(request.getAmountInWords());
        contract.setPaymentDueDate(request.getPaymentDueDate());
        contract.setPaymentMethod(request.getPaymentMethod());
        contract.setDeliverySchedule(request.getDeliverySchedule());
        contract.setShippingResponsibility(request.getShippingResponsibility());
        contract.setUnloadingCost(request.getUnloadingCost());
        contract.setStorageFeePerDay(request.getStorageFeePerDay());
        contract.setInspectionAgency(request.getInspectionAgency());
        contract.setWarrantyProductScope(request.getWarrantyProductScope());
        contract.setWarrantyMonths(request.getWarrantyMonths());
        contract.setPenaltyRate(request.getPenaltyRate());
        contract.setContractCopies(request.getContractCopies());
        contract.setCopiesPerParty(request.getCopiesPerParty());
        contract.setGeneralTerms(request.getGeneralTerms());
        contract.setStatus(Contract.ContractStatus.ADMIN_REVIEWED);

        contractRepository.save(contract);
    }

    @Override
    @Transactional
    public void sendToCustomer(Long contractId, String username) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if (contract.getStatus() != Contract.ContractStatus.ADMIN_REVIEWED) {
            throw new RuntimeException("Only admin reviewed contract can be sent to customer");
        }

        Employee sale = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Sale employee not found"));

        if (!canReviewContract(sale) && (contract.getSale() == null || !contract.getSale().getId().equals(sale.getId()))) {
            throw new RuntimeException("Only owner sale can send contract to customer");
        }

        if (!hasContractRules(contract)) {
            throw new RuntimeException("Please complete contract rules before sending to customer");
        }

        contract.setAssignedEmployee(sale);
        contract.setStatus(Contract.ContractStatus.SENT_TO_CUSTOMER);
        contractRepository.save(contract);
    }

    @Override
    @Transactional
    public void customerSignContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if (contract.getStatus() != Contract.ContractStatus.SENT_TO_CUSTOMER) {
            throw new RuntimeException("Only contract sent to customer can be signed");
        }

        contract.setStatus(Contract.ContractStatus.SIGNED);
        contract.setSignedAt(LocalDateTime.now());
        contractRepository.save(contract);
    }

    @Override
    @Transactional
    public void cancelContract(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if (contract.getStatus() == Contract.ContractStatus.SIGNED) {
            throw new RuntimeException("Signed contract cannot be cancelled");
        }

        contract.setStatus(Contract.ContractStatus.CANCELLED);
        contractRepository.save(contract);
    }
    @Override
    @Transactional(readOnly = true)
    public Page<ContractResponse> searchContracts(
            String username,
            String keyword,
            Contract.ContractStatus status,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Employee employee = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Contract> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (canReviewContract(employee)) {
                predicates.add(cb.equal(root.get("adminOfficer").get("id"), employee.getId()));
            } else {
                predicates.add(cb.equal(root.get("sale").get("id"), employee.getId()));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                Join<Contract, Customer> customer = root.join("customer", JoinType.LEFT);

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("contractCode")), like),
                        cb.like(cb.lower(customer.get("name")), like),
                        cb.like(cb.lower(customer.get("companyName")), like),
                        cb.like(cb.lower(customer.get("fullName")), like),
                        cb.like(cb.lower(customer.get("email")), like)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return contractRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ContractStatisticsResponse getContractStatistics(String username) {
        Employee employee = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Long employeeId = employee.getId();

        if (canReviewContract(employee)) {
            return ContractStatisticsResponse.builder()
                    .total(contractRepository.countByAdminOfficerId(employeeId))
                    .draft(contractRepository.countByAdminOfficerIdAndStatus(employeeId, Contract.ContractStatus.DRAFT))
                    .pending(contractRepository.countByAdminOfficerIdAndStatus(employeeId, Contract.ContractStatus.PENDING_ADMIN_OFFICER))
                    .reviewed(contractRepository.countByAdminOfficerIdAndStatus(employeeId, Contract.ContractStatus.ADMIN_REVIEWED))
                    .sent(contractRepository.countByAdminOfficerIdAndStatus(employeeId, Contract.ContractStatus.SENT_TO_CUSTOMER))
                    .signed(contractRepository.countByAdminOfficerIdAndStatus(employeeId, Contract.ContractStatus.SIGNED))
                    .cancelled(contractRepository.countByAdminOfficerIdAndStatus(employeeId, Contract.ContractStatus.CANCELLED))
                    .build();
        }

        return ContractStatisticsResponse.builder()
                .total(contractRepository.countBySaleId(employeeId))
                .draft(contractRepository.countBySaleIdAndStatus(employeeId, Contract.ContractStatus.DRAFT))
                .pending(contractRepository.countBySaleIdAndStatus(employeeId, Contract.ContractStatus.PENDING_ADMIN_OFFICER))
                .reviewed(contractRepository.countBySaleIdAndStatus(employeeId, Contract.ContractStatus.ADMIN_REVIEWED))
                .sent(contractRepository.countBySaleIdAndStatus(employeeId, Contract.ContractStatus.SENT_TO_CUSTOMER))
                .signed(contractRepository.countBySaleIdAndStatus(employeeId, Contract.ContractStatus.SIGNED))
                .cancelled(contractRepository.countBySaleIdAndStatus(employeeId, Contract.ContractStatus.CANCELLED))
                .build();
    }

    @Override
    @Transactional
    public void deleteContract(Long contractId, String username) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        if (contract.getStatus() == Contract.ContractStatus.SIGNED) {
            throw new RuntimeException("Signed contract cannot be deleted");
        }

        contract.setStatus(Contract.ContractStatus.CANCELLED);
        contractRepository.save(contract);
    }

    private ContractResponse mapToResponse(Contract contract) {
        ContractResponse response = new ContractResponse();

        response.setId(contract.getId());
        response.setContractCode(contract.getContractCode());
        response.setStatus(contract.getStatus().name());
        response.setContractAmount(defaultMoney(contract.getContractAmount()));
        response.setDiscountAmount(defaultMoney(contract.getDiscountAmount()));
        response.setFinalAmount(defaultMoney(contract.getFinalAmount()));
        response.setCreatedAt(contract.getCreatedAt());
        response.setSignedAt(contract.getSignedAt());
        response.setContractStartDate(contract.getContractStartDate());
        response.setContractEndDate(contract.getContractEndDate());
        response.setPaymentTerms(contract.getPaymentTerms());
        response.setDeliveryTerms(contract.getDeliveryTerms());
        response.setLegalTerms(contract.getLegalTerms());
        response.setAdminNote(contract.getAdminNote());
        response.setSigningDate(contract.getSigningDate());
        response.setSigningPlace(contract.getSigningPlace());
        response.setSellerCompanyName(contract.getSellerCompanyName());
        response.setSellerTaxCode(contract.getSellerTaxCode());
        response.setSellerAddress(contract.getSellerAddress());
        response.setSellerPhone(contract.getSellerPhone());
        response.setSellerFax(contract.getSellerFax());
        response.setSellerBankAccount(contract.getSellerBankAccount());
        response.setSellerBankName(contract.getSellerBankName());
        response.setSellerRepresentativeName(contract.getSellerRepresentativeName());
        response.setSellerRepresentativeTitle(contract.getSellerRepresentativeTitle());
        response.setSellerIdentityNumber(contract.getSellerIdentityNumber());
        response.setSellerIdentityIssuedPlace(contract.getSellerIdentityIssuedPlace());
        response.setSellerIdentityIssuedDate(contract.getSellerIdentityIssuedDate());
        response.setSellerAuthorizationInfo(contract.getSellerAuthorizationInfo());
        response.setBuyerCompanyName(contract.getBuyerCompanyName());
        response.setBuyerTaxCode(contract.getBuyerTaxCode());
        response.setBuyerAddress(contract.getBuyerAddress());
        response.setBuyerPhone(contract.getBuyerPhone());
        response.setBuyerFax(contract.getBuyerFax());
        response.setBuyerBankAccount(contract.getBuyerBankAccount());
        response.setBuyerBankName(contract.getBuyerBankName());
        response.setBuyerRepresentativeName(contract.getBuyerRepresentativeName());
        response.setBuyerRepresentativeTitle(contract.getBuyerRepresentativeTitle());
        response.setBuyerIdentityNumber(contract.getBuyerIdentityNumber());
        response.setBuyerIdentityIssuedPlace(contract.getBuyerIdentityIssuedPlace());
        response.setBuyerIdentityIssuedDate(contract.getBuyerIdentityIssuedDate());
        response.setBuyerAuthorizationInfo(contract.getBuyerAuthorizationInfo());
        response.setAmountInWords(contract.getAmountInWords());
        response.setPaymentDueDate(contract.getPaymentDueDate());
        response.setPaymentMethod(contract.getPaymentMethod());
        response.setDeliverySchedule(contract.getDeliverySchedule());
        response.setShippingResponsibility(contract.getShippingResponsibility());
        response.setUnloadingCost(contract.getUnloadingCost());
        response.setStorageFeePerDay(contract.getStorageFeePerDay());
        response.setInspectionAgency(contract.getInspectionAgency());
        response.setWarrantyProductScope(contract.getWarrantyProductScope());
        response.setWarrantyMonths(contract.getWarrantyMonths());
        response.setPenaltyRate(contract.getPenaltyRate());
        response.setContractCopies(contract.getContractCopies());
        response.setCopiesPerParty(contract.getCopiesPerParty());
        response.setGeneralTerms(contract.getGeneralTerms());

        Quotation quotation = contract.getQuotation();
        if (quotation != null) {
            response.setQuotationId(quotation.getId());
            response.setQuotationCode(quotation.getQuotationCode());
            response.setItems(mapContractItems(quotation.getDetails()));
        }

        Customer customer = contract.getCustomer();
        if (customer != null) {
            response.setCustomerId(customer.getId());
            response.setCustomerName(resolveCustomerName(customer));
            response.setCustomerEmail(customer.getEmail());
            response.setCustomerPhone(customer.getPhone());
            response.setCustomerAddress(customer.getAddress());
            response.setCustomerAccountCreated(hasText(customer.getPasswordHash()));
        }

        Employee sale = contract.getSale();
        if (sale != null) {
            response.setSaleId(sale.getId());
            response.setSaleName(resolveEmployeeName(sale));
        }

        Employee adminOfficer = contract.getAdminOfficer();
        if (adminOfficer != null) {
            response.setAdminOfficerId(adminOfficer.getId());
            response.setAdminOfficerName(resolveEmployeeName(adminOfficer));
        }

        return response;
    }

    private List<ContractItemResponse> mapContractItems(List<QuotationDetail> quotationDetails) {
        if (quotationDetails == null) {
            return List.of();
        }

        return quotationDetails.stream()
                .map(this::mapContractItem)
                .toList();
    }

    private ContractItemResponse mapContractItem(QuotationDetail detail) {
        ContractItemResponse item = new ContractItemResponse();

        Product product = detail.getProduct();
        if (product != null) {
            item.setProductId(product.getId());
            item.setProductCode(product.getProductCode());
            item.setProductName(product.getName());
        } else {
            item.setProductName(detail.getServiceName());
        }

        item.setDescription(detail.getDescription());
        item.setQuantity(detail.getQuantity());
        item.setUnitPrice(defaultMoney(detail.getUnitPrice()));
        item.setTotalPrice(defaultMoney(detail.getTotalPrice()));

        return item;
    }

    private String resolveCustomerName(Customer customer) {
        if (hasText(customer.getName())) {
            return customer.getName();
        }
        if (hasText(customer.getCompanyName())) {
            return customer.getCompanyName();
        }
        return customer.getFullName();
    }

    private String resolveEmployeeName(Employee employee) {
        if (employee.getUser() != null) {
            if (hasText(employee.getUser().getFullName())) {
                return employee.getUser().getFullName();
            }
            if (hasText(employee.getUser().getUsername())) {
                return employee.getUser().getUsername();
            }
            if (hasText(employee.getUser().getEmail())) {
                return employee.getUser().getEmail();
            }
        }

        return employee.getEmployeeCode();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean canReviewContract(Employee employee) {
        if (employee == null || employee.getEmployeeType() == null) {
            return false;
        }

        return ADMIN_OFFICER_TYPE.equalsIgnoreCase(employee.getEmployeeType())
                || ADMIN_OFFICER_TYPE_ALIAS.equalsIgnoreCase(employee.getEmployeeType())
                || ADMIN_TYPE.equalsIgnoreCase(employee.getEmployeeType());
    }

    private boolean hasContractRules(Contract contract) {
        return contract.getContractStartDate() != null
                && contract.getContractEndDate() != null
                && hasText(contract.getPaymentTerms())
                && hasText(contract.getLegalTerms());
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
