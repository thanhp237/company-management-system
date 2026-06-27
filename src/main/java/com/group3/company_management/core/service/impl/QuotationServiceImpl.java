package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.QuotationDetailRequest;
import com.group3.company_management.core.dto.QuotationDetailResponse;
import com.group3.company_management.core.dto.QuotationRequest;
import com.group3.company_management.core.dto.QuotationResponse;
import com.group3.company_management.core.entity.Customer;
import com.group3.company_management.core.entity.Product;
import com.group3.company_management.core.entity.Quotation;
import com.group3.company_management.core.entity.QuotationDetail;
import com.group3.company_management.core.entity.User;
import com.group3.company_management.core.entity.Voucher;
import com.group3.company_management.core.repository.CustomerRepository;
import com.group3.company_management.core.repository.OpportunityRepository;
import com.group3.company_management.core.repository.ProductRepository;
import com.group3.company_management.core.repository.QuotationRepository;
import com.group3.company_management.core.repository.UserRepository;
import com.group3.company_management.core.repository.VoucherRepository;
import com.group3.company_management.core.service.QuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuotationServiceImpl implements QuotationService {
    private static final Set<String> QUOTATION_STAGES = Set.of("QUALIFIED", "PROPOSAL", "NEGOTIATION");

    private final VoucherRepository voucherRepository;
    private final QuotationRepository quotationRepository;
    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Long createQuotation(QuotationRequest request, String username) {
        validateOpportunityQuotation(request, username);

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Quotation quotation = new Quotation();
        quotation.setCustomer(customer);
        quotation.setVoucherId(request.getVoucherId());
        quotation.setOpportunityId(request.getOpportunityId());
        quotation.setNote(request.getNote());
        quotation.setStatus("DRAFT");

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        quotation.setEmployeeId(user.getEmployee().getId());

        BigDecimal subTotal = BigDecimal.ZERO;

        for (QuotationDetailRequest item : request.getDetails()) {

            if (item.getProductId() == null
                    || item.getQuantity() == null
                    || item.getQuantity() <= 0) {
                continue;
            }

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            BigDecimal unitPrice = product.getUnitPrice();
            BigDecimal quantity = BigDecimal.valueOf(item.getQuantity());
            BigDecimal totalPrice = unitPrice.multiply(quantity);

            QuotationDetail detail = new QuotationDetail();
            detail.setQuotation(quotation);
            detail.setProduct(product);
            detail.setServiceName(product.getName());
            detail.setDescription(product.getDescription());
            detail.setQuantity(item.getQuantity());
            detail.setUnitPrice(unitPrice);
            detail.setTotalPrice(totalPrice);

            quotation.getDetails().add(detail);

            subTotal = subTotal.add(totalPrice);
        }

        BigDecimal discountAmount = calculateDiscountAmount(request.getVoucherId(), subTotal);

        quotation.setDiscountAmount(discountAmount);
        quotation.setSubTotal(subTotal);
        quotation.setFinalAmount(subTotal.subtract(discountAmount));
        quotation.setQuotationCode("QT-" + System.currentTimeMillis());

        Quotation saved = quotationRepository.save(quotation);

        return saved.getId();
    }

    private void validateOpportunityQuotation(QuotationRequest request, String username) {
        if (request.getOpportunityId() == null) {
            return;
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        var opportunity = opportunityRepository.findDetailById(request.getOpportunityId())
                .orElseThrow(() -> new RuntimeException("Opportunity not found"));

        if (!canManageOpportunity(user)
                && (opportunity.getAssignedTo() == null
                || !username.equals(opportunity.getAssignedTo().getUsername()))) {
            throw new RuntimeException("You are not allowed to create quotation for this opportunity");
        }
        if (opportunity.getStage() == null
                || !QUOTATION_STAGES.contains(opportunity.getStage().toUpperCase(Locale.ROOT))) {
            throw new RuntimeException("Opportunity stage is not ready for quotation");
        }
        if (quotationRepository.findFirstByOpportunityIdOrderByCreatedAtDesc(request.getOpportunityId()).isPresent()) {
            throw new RuntimeException("Quotation already exists for this opportunity");
        }
        if (request.getCustomerId() != null
                && opportunity.getCustomer() != null
                && !request.getCustomerId().equals(opportunity.getCustomer().getId())) {
            throw new RuntimeException("Quotation customer does not match opportunity customer");
        }
    }

    private boolean canManageOpportunity(User user) {
        if (user.getRole() == null || user.getRole().getRoleCode() == null) {
            return false;
        }
        String roleCode = user.getRole().getRoleCode().toUpperCase(Locale.ROOT);
        return "ADMIN".equals(roleCode) || "MANAGER".equals(roleCode) || "SALES_MANAGER".equals(roleCode);
    }

    @Override
    public QuotationResponse getQuotationDetail(Long id) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quotation not found"));

        return mapToResponse(quotation);
    }

    @Override
    public QuotationResponse previewQuotation(QuotationRequest request) {

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        QuotationResponse response = new QuotationResponse();
        response.setCustomerId(customer.getId());
        response.setCustomerName(customer.getName());
        response.setCustomerEmail(customer.getEmail());
        response.setCustomerPhone(customer.getPhone());
        response.setCustomerAddress(customer.getAddress());
        response.setNote(request.getNote());
        response.setStatus("PREVIEW");

        BigDecimal subTotal = BigDecimal.ZERO;
        List<QuotationDetailResponse> detailResponses = new ArrayList<>();

        for (QuotationDetailRequest item : request.getDetails()) {

            if (item.getProductId() == null
                    || item.getQuantity() == null
                    || item.getQuantity() <= 0) {
                continue;
            }

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            BigDecimal unitPrice = product.getUnitPrice();
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));

            QuotationDetailResponse detailResponse = new QuotationDetailResponse();
            detailResponse.setProductId(product.getId());
            detailResponse.setProductCode(product.getProductCode());
            detailResponse.setProductName(product.getName());
            detailResponse.setDescription(product.getDescription());
            detailResponse.setQuantity(item.getQuantity());
            detailResponse.setUnitPrice(unitPrice);
            detailResponse.setTotalPrice(totalPrice);

            detailResponses.add(detailResponse);
            subTotal = subTotal.add(totalPrice);
        }

        BigDecimal discountAmount = calculateDiscountAmount(request.getVoucherId(), subTotal);

        response.setDetails(detailResponses);
        response.setSubTotal(subTotal);
        response.setDiscountAmount(discountAmount);
        response.setFinalAmount(subTotal.subtract(discountAmount));

        return response;
    }

    private BigDecimal calculateDiscountAmount(Long voucherId, BigDecimal subTotal) {
        if (voucherId == null) {
            return BigDecimal.ZERO;
        }

        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Voucher not found"));

        BigDecimal discountAmount = subTotal
                .multiply(voucher.getDiscountPercent())
                .divide(BigDecimal.valueOf(100));

        if (voucher.getMaxDiscountAmount() != null
                && discountAmount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
            discountAmount = voucher.getMaxDiscountAmount();
        }

        return discountAmount;
    }

    private QuotationResponse mapToResponse(Quotation quotation) {
        QuotationResponse response = new QuotationResponse();

        response.setId(quotation.getId());
        response.setQuotationCode(quotation.getQuotationCode());

        Customer customer = quotation.getCustomer();
        response.setCustomerId(customer.getId());
        response.setCustomerName(customer.getName());
        response.setCustomerEmail(customer.getEmail());
        response.setCustomerPhone(customer.getPhone());
        response.setCustomerAddress(customer.getAddress());

        response.setSubTotal(quotation.getSubTotal());
        response.setDiscountAmount(quotation.getDiscountAmount());
        response.setFinalAmount(quotation.getFinalAmount());
        response.setStatus(quotation.getStatus());
        response.setNote(quotation.getNote());
        response.setCreatedAt(quotation.getCreatedAt());

        List<QuotationDetailResponse> details = quotation.getDetails()
                .stream()
                .map(detail -> {
                    Product product = detail.getProduct();

                    QuotationDetailResponse d = new QuotationDetailResponse();
                    d.setProductId(product.getId());
                    d.setProductCode(product.getProductCode());
                    d.setProductName(product.getName());
                    d.setDescription(detail.getDescription());
                    d.setQuantity(detail.getQuantity());
                    d.setUnitPrice(detail.getUnitPrice());
                    d.setTotalPrice(detail.getTotalPrice());

                    return d;
                })
                .toList();

        response.setDetails(details);

        return response;
    }
}
