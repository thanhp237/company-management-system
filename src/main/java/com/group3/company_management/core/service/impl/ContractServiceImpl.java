package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.ContractItemResponse;
import com.group3.company_management.core.dto.ContractRuleRequest;
import com.group3.company_management.core.dto.ContractResponse;
import com.group3.company_management.core.dto.PaymentScheduleRequest;
import com.group3.company_management.core.dto.PaymentScheduleResponse;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Contract;
import com.group3.company_management.core.entity.Employee;
import com.group3.company_management.core.entity.Opportunity;
import com.group3.company_management.core.entity.Product;
import com.group3.company_management.core.entity.Quotation;
import com.group3.company_management.core.entity.QuotationDetail;
import com.group3.company_management.core.entity.PaymentSchedule;
import com.group3.company_management.core.repository.ContractRepository;
import com.group3.company_management.core.repository.EmployeeRepository;
import com.group3.company_management.core.repository.OpportunityRepository;
import com.group3.company_management.core.repository.QuotationRepository;
import com.group3.company_management.core.repository.VoucherRepository;
import com.group3.company_management.core.repository.PaymentScheduleRepository;
import com.group3.company_management.core.repository.InvoiceRepository;
import com.group3.company_management.core.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;
import java.util.Locale;
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

    private static final Set<String> CONTRACT_READY_QUOTATION_STATUSES = Set.of("APPROVED", "ACCEPTED");
    private static final String ADMIN_OFFICER_TYPE = "ADMIN_OFFICER";
    private static final String ADMIN_OFFICER_TYPE_ALIAS = "ADMINOFFICER";
    private static final String ADMIN_TYPE = "ADMIN";

    private final QuotationRepository quotationRepository;
    private final ContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;
    private final OpportunityRepository opportunityRepository;
    private final VoucherRepository voucherRepository;
    private final PaymentScheduleRepository paymentScheduleRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional
    public Long createFromQuotation(Long quotationId, String username) {
        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo giá."));

        if (quotation.getStatus() == null
                || !CONTRACT_READY_QUOTATION_STATUSES.contains(quotation.getStatus().trim().toUpperCase())) {
            throw new RuntimeException("Chỉ báo giá đã duyệt hoặc đã chấp nhận mới có thể tạo hợp đồng.");
        }
        if (quotation.getOpportunityId() == null) {
            throw new RuntimeException("Báo giá phải thuộc một cơ hội trước khi tạo hợp đồng.");
        }
        Opportunity opportunity = opportunityRepository.findById(quotation.getOpportunityId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ hội."));
        if (opportunity.getStage() == null || !"WON".equalsIgnoreCase(opportunity.getStage())) {
            throw new RuntimeException("Chỉ cơ hội đã thắng mới có thể tạo hợp đồng.");
        }

        contractRepository.findByQuotationId(quotationId)
                .ifPresent(existingContract -> {
                    throw new RuntimeException("Báo giá này đã có hợp đồng.");
                });

        Employee sale = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên kinh doanh."));

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
        fillBuyerDefaults(contract, quotation.getCustomer());

        Contract savedContract = contractRepository.save(contract);

        return savedContract.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getContractDetail(Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng."));

        return mapToResponse(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContractResponse> getMyContracts(String username) {
        Employee employee = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên."));

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
    @Transactional(readOnly = true)
    public List<ContractResponse> getCustomerContracts(Long customerId) {
        if (customerId == null) {
            throw new RuntimeException("Thiếu thông tin khách hàng.");
        }

        return contractRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ContractResponse getCustomerContractDetail(Long contractId, Long customerId) {
        Contract contract = getCustomerOwnedContract(contractId, customerId);
        return mapToResponse(contract);
    }

    @Override
    @Transactional
    public void submitToAdmin(Long contractId, String username) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng."));
        Employee employee = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên."));

        validateSalesStepAccess(contract, employee);

        if (contract.getStatus() != Contract.ContractStatus.DRAFT) {
            throw new RuntimeException("Chỉ hợp đồng nháp mới có thể gửi cho hành chính hợp đồng.");
        }

        contract.setStatus(Contract.ContractStatus.PENDING_ADMIN_OFFICER);
        contractRepository.save(contract);
    }

    @Override
    @Transactional
    public void updateDraftContractInfo(Long contractId, ContractRuleRequest request, String username) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng."));
        Employee employee = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên kinh doanh."));

        validateSalesStepAccess(contract, employee);

        if (contract.getStatus() != Contract.ContractStatus.DRAFT) {
            throw new RuntimeException("Chỉ hợp đồng nháp mới được sale chỉnh sửa trước khi gửi hành chính hợp đồng.");
        }

        contract.setBuyerCompanyName(normalize(request.getBuyerCompanyName()));
        contract.setBuyerTaxCode(normalize(request.getBuyerTaxCode()));
        contract.setBuyerAddress(normalize(request.getBuyerAddress()));
        contract.setBuyerPhone(normalize(request.getBuyerPhone()));
        contract.setBuyerFax(normalize(request.getBuyerFax()));
        contract.setBuyerBankAccount(normalize(request.getBuyerBankAccount()));
        contract.setBuyerBankName(normalize(request.getBuyerBankName()));
        contract.setBuyerRepresentativeName(normalize(request.getBuyerRepresentativeName()));
        contract.setBuyerRepresentativeTitle(normalize(request.getBuyerRepresentativeTitle()));
        contract.setBuyerIdentityNumber(normalize(request.getBuyerIdentityNumber()));
        contract.setBuyerIdentityIssuedPlace(normalize(request.getBuyerIdentityIssuedPlace()));
        contract.setBuyerIdentityIssuedDate(request.getBuyerIdentityIssuedDate());
        contract.setBuyerAuthorizationInfo(normalize(request.getBuyerAuthorizationInfo()));
        contract.setSigningPlace(normalize(request.getSigningPlace()));

        contractRepository.save(contract);
    }

    @Override
    @Transactional
    public void updateContractRules(Long contractId, ContractRuleRequest request, String adminUsername) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng."));

        if (contract.getStatus() != Contract.ContractStatus.PENDING_ADMIN_OFFICER
                && contract.getStatus() != Contract.ContractStatus.REVISION_REQUESTED) {
            throw new RuntimeException("Chỉ hợp đồng đang chờ hành chính hợp đồng mới được cập nhật điều khoản.");
        }

        Employee adminOfficer = employeeRepository.findByUser_Username(adminUsername)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên hành chính hợp đồng."));

        if (!canReviewContract(adminOfficer)) {
            throw new RuntimeException("Chỉ hành chính hợp đồng mới được cập nhật điều khoản hợp đồng.");
        }

        contract.setAdminOfficer(adminOfficer);
        contract.setAssignedEmployee(adminOfficer);
        contract.setContractStartDate(request.getContractStartDate());
        contract.setContractEndDate(request.getContractEndDate());
        contract.setPaymentTerms(request.getPaymentTerms());
        Contract.PaymentPlanType planType = parsePlanType(request.getPaymentPlanType());
        List<PaymentScheduleRequest> schedules = validateSchedules(planType, request.getPaymentSchedules(), contract.getFinalAmount());
        contract.setPaymentPlanType(planType);
        contract.setRevisionReason(null);
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
        paymentScheduleRepository.deleteByContractId(contractId);
        paymentScheduleRepository.flush();
        for (int i = 0; i < schedules.size(); i++) {
            PaymentScheduleRequest row = schedules.get(i);
            paymentScheduleRepository.save(PaymentSchedule.builder()
                    .contract(contract).installmentNo(i + 1).dueDate(row.getDueDate())
                    .amount(row.getAmount()).description(normalize(row.getDescription())).build());
        }
    }

    @Override
    @Transactional
    public void sendToCustomer(Long contractId, String username) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng."));

        if (contract.getStatus() != Contract.ContractStatus.ADMIN_REVIEWED) {
            throw new RuntimeException("Chỉ hợp đồng đã duyệt điều khoản mới có thể gửi cho khách hàng.");
        }

        Employee sale = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên kinh doanh."));

        if (!canViewAllContracts(sale) && (contract.getSale() == null || !contract.getSale().getId().equals(sale.getId()))) {
            throw new RuntimeException("Chỉ nhân viên kinh doanh phụ trách mới được gửi hợp đồng cho khách hàng.");
        }

        if (!hasContractRules(contract)) {
            throw new RuntimeException("Vui lòng nhập đầy đủ điều khoản trước khi gửi hợp đồng cho khách hàng.");
        }

        contract.setAssignedEmployee(sale);
        contract.setStatus(Contract.ContractStatus.SENT_TO_CUSTOMER);
        contractRepository.save(contract);
    }

    @Override
    @Transactional
    public void customerSignContractByCustomer(Long contractId, Long customerId) {
        Contract contract = getCustomerOwnedContract(contractId, customerId);

        if (contract.getStatus() != Contract.ContractStatus.SENT_TO_CUSTOMER) {
            throw new RuntimeException("Chỉ hợp đồng đã gửi cho khách hàng mới có thể ký.");
        }

        ensurePaymentScheduleExists(contract);
        contract.setStatus(Contract.ContractStatus.SIGNED);
        contract.setSignedAt(LocalDateTime.now());
        contractRepository.save(contract);
    }

    @Override
    @Transactional
    public void customerRequestRevision(Long contractId, Long customerId, String reason) {
        Contract contract = getCustomerOwnedContract(contractId, customerId);
        if (contract.getStatus() != Contract.ContractStatus.SENT_TO_CUSTOMER) {
            throw new RuntimeException("Hợp đồng không ở trạng thái chờ phản hồi.");
        }
        if (!hasText(reason)) throw new RuntimeException("Vui lòng nhập nội dung cần chỉnh sửa.");
        contract.setRevisionReason(reason.trim());
        contract.setStatus(Contract.ContractStatus.REVISION_REQUESTED);
        contract.setAssignedEmployee(contract.getAdminOfficer());
        contractRepository.save(contract);
    }

    @Override
    @Transactional
    public void cancelContract(Long contractId, String username) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng."));
        Employee employee = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên."));

        validateSalesStepAccess(contract, employee);

        if (contract.getStatus() == Contract.ContractStatus.SIGNED) {
            throw new RuntimeException("Hợp đồng đã ký không thể hủy.");
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên."));

        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Contract> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (canViewAllContracts(employee)) {
                // Managers and admins can review the full contract workspace.
            } else if (canReviewContract(employee)) {
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        Long employeeId = employee.getId();

        if (canViewAllContracts(employee)) {
            return ContractStatisticsResponse.builder()
                    .total(contractRepository.count())
                    .draft(contractRepository.countByStatus(Contract.ContractStatus.DRAFT))
                    .pending(contractRepository.countByStatus(Contract.ContractStatus.PENDING_ADMIN_OFFICER))
                    .reviewed(contractRepository.countByStatus(Contract.ContractStatus.ADMIN_REVIEWED))
                    .sent(contractRepository.countByStatus(Contract.ContractStatus.SENT_TO_CUSTOMER))
                    .signed(contractRepository.countByStatus(Contract.ContractStatus.SIGNED))
                    .cancelled(contractRepository.countByStatus(Contract.ContractStatus.CANCELLED))
                    .build();
        }

        if (canReviewContract(employee)) {
            long pendingForReview = contractRepository.countByStatus(Contract.ContractStatus.PENDING_ADMIN_OFFICER);
            return ContractStatisticsResponse.builder()
                    .total(contractRepository.countByAdminOfficerId(employeeId) + pendingForReview)
                    .draft(contractRepository.countByAdminOfficerIdAndStatus(employeeId, Contract.ContractStatus.DRAFT))
                    .pending(pendingForReview)
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng."));
        Employee employee = employeeRepository.findByUser_Username(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên."));

        validateSalesStepAccess(contract, employee);

        if (contract.getStatus() == Contract.ContractStatus.SIGNED) {
            throw new RuntimeException("Hợp đồng đã ký không thể hủy.");
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
        response.setPaymentPlanType(contract.getPaymentPlanType() == null ? null : contract.getPaymentPlanType().name());
        response.setRevisionReason(contract.getRevisionReason());
        response.setPaymentSchedules(paymentScheduleRepository.findByContractIdOrderByInstallmentNo(contract.getId())
                .stream().map(schedule -> {
                    var invoice = invoiceRepository.findByPaymentScheduleId(schedule.getId()).orElse(null);
                    return PaymentScheduleResponse.builder().id(schedule.getId())
                            .installmentNo(schedule.getInstallmentNo()).dueDate(schedule.getDueDate())
                            .amount(schedule.getAmount()).description(schedule.getDescription())
                            .invoiceId(invoice == null ? null : invoice.getId())
                            .invoiceStatus(invoice == null ? null : invoice.getStatus().name()).build();
                }).toList());
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
            response.setVoucherId(quotation.getVoucherId());
            if (quotation.getVoucherId() != null) {
                voucherRepository.findById(quotation.getVoucherId()).ifPresent(voucher -> {
                    response.setVoucherCode(voucher.getVoucherCode());
                    response.setVoucherDiscountPercent(voucher.getDiscountPercent());
                    response.setVoucherMaxDiscountAmount(voucher.getMaxDiscountAmount());
                });
            }
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

    private Contract getCustomerOwnedContract(Long contractId, Long customerId) {
        if (customerId == null) {
            throw new RuntimeException("Thiếu thông tin khách hàng.");
        }

        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng."));

        if (contract.getCustomer() == null || !customerId.equals(contract.getCustomer().getId())) {
            throw new RuntimeException("Bạn không có quyền truy cập hợp đồng này.");
        }

        return contract;
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
            item.setImageUrl(product.getImageUrl());
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

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void fillBuyerDefaults(Contract contract, Customer customer) {
        if (customer == null) {
            return;
        }
        contract.setBuyerCompanyName(firstText(customer.getCompanyName(), customer.getName(), customer.getFullName()));
        contract.setBuyerTaxCode(customer.getTaxCode());
        contract.setBuyerAddress(customer.getAddress());
        contract.setBuyerPhone(customer.getPhone());
        contract.setBuyerRepresentativeName(customer.getFullName());
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private boolean canReviewContract(Employee employee) {
        if (employee == null || employee.getEmployeeType() == null) {
            return false;
        }

        return ADMIN_OFFICER_TYPE.equalsIgnoreCase(employee.getEmployeeType())
                || ADMIN_OFFICER_TYPE_ALIAS.equalsIgnoreCase(employee.getEmployeeType())
                || ADMIN_TYPE.equalsIgnoreCase(employee.getEmployeeType());
    }

    private boolean canViewAllContracts(Employee employee) {
        String roleCode = roleCode(employee);
        return "ADMIN".equals(roleCode)
                || "MANAGER".equals(roleCode)
                || "SALES_MANAGER".equals(roleCode)
                || "ACCOUNTANT".equals(roleCode)
                || "DIRECTOR".equals(roleCode);
    }

    private void ensurePaymentScheduleExists(Contract contract) {
        if (contract == null || contract.getId() == null
                || !paymentScheduleRepository.findByContractIdOrderByInstallmentNo(contract.getId()).isEmpty()) {
            return;
        }

        if (contract.getPaymentPlanType() == null) {
            contract.setPaymentPlanType(Contract.PaymentPlanType.ONE_TIME);
        }

        paymentScheduleRepository.save(PaymentSchedule.builder()
                .contract(contract)
                .installmentNo(1)
                .dueDate(contract.getPaymentDueDate() != null ? contract.getPaymentDueDate() : LocalDate.now())
                .amount(defaultMoney(contract.getFinalAmount()))
                .description("Thanh toán toàn bộ hợp đồng")
                .build());
    }

    private void validateSalesStepAccess(Contract contract, Employee employee) {
        if (canViewAllContracts(employee)) {
            return;
        }
        if (contract.getSale() == null || employee == null || !employee.getId().equals(contract.getSale().getId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật hợp đồng này");
        }
    }

    private String roleCode(Employee employee) {
        if (employee == null || employee.getUser() == null || employee.getUser().getRole() == null
                || employee.getUser().getRole().getRoleCode() == null) {
            return "";
        }
        return employee.getUser().getRole().getRoleCode().trim().toUpperCase(Locale.ROOT);
    }

    private boolean hasContractRules(Contract contract) {
        return contract.getContractStartDate() != null
                && contract.getContractEndDate() != null
                && hasText(contract.getPaymentTerms())
                && contract.getPaymentPlanType() != null
                && !paymentScheduleRepository.findByContractIdOrderByInstallmentNo(contract.getId()).isEmpty()
                && hasText(contract.getLegalTerms());
    }

    private Contract.PaymentPlanType parsePlanType(String value) {
        try {
            return Contract.PaymentPlanType.valueOf(value);
        } catch (Exception e) {
            throw new RuntimeException("Vui lòng chọn hình thức thanh toán.");
        }
    }

    private List<PaymentScheduleRequest> validateSchedules(Contract.PaymentPlanType type,
                                                            List<PaymentScheduleRequest> rows,
                                                            BigDecimal finalAmount) {
        List<PaymentScheduleRequest> valid = rows == null ? List.of() : rows.stream()
                .filter(r -> r != null && (r.getAmount() != null || r.getDueDate() != null)).toList();
        if (type == Contract.PaymentPlanType.ONE_TIME && valid.size() != 1)
            throw new RuntimeException("Thanh toán một lần phải có đúng một mốc.");
        if (type == Contract.PaymentPlanType.INSTALLMENTS && valid.size() < 2)
            throw new RuntimeException("Thanh toán theo đợt phải có ít nhất hai đợt.");
        BigDecimal total = BigDecimal.ZERO;
        for (PaymentScheduleRequest row : valid) {
            if (row.getDueDate() == null || row.getAmount() == null || row.getAmount().signum() <= 0)
                throw new RuntimeException("Mỗi đợt phải có hạn thanh toán và số tiền lớn hơn 0.");
            total = total.add(row.getAmount());
        }
        if (total.compareTo(defaultMoney(finalAmount)) != 0)
            throw new RuntimeException("Tổng lịch thanh toán phải bằng tổng giá trị hợp đồng.");
        return valid;
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
