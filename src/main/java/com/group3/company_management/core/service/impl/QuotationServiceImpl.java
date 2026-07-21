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
import java.time.LocalDateTime;
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        Quotation quotation = new Quotation();
        quotation.setCustomer(customer);
        quotation.setVoucherId(request.getVoucherId());
        quotation.setOpportunityId(request.getOpportunityId());
        quotation.setNote(request.getNote());
        quotation.setStatus("DRAFT");
        quotation.setValidUntil(request.getValidUntil());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản nhân viên"));
        quotation.setEmployeeId(user.getEmployee().getId());

        BigDecimal subTotal = BigDecimal.ZERO;
        List<QuotationDetailRequest> details = requireValidDetails(request.getDetails());

        for (QuotationDetailRequest item : details) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new RuntimeException("Sản phẩm đã ngừng hoạt động, không thể thêm vào báo giá");
            }

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
        quotation.setQuotationCode(request.getQuotationCode());

        Quotation saved = quotationRepository.save(quotation);

        return saved.getId();
    }

    private List<QuotationDetailRequest> requireValidDetails(List<QuotationDetailRequest> details) {
        if (details == null || details.isEmpty()) {
            throw new RuntimeException("Báo giá phải có ít nhất một sản phẩm hợp lệ");
        }

        List<QuotationDetailRequest> selectedDetails = details.stream()
                .filter(item -> item != null && item.isSelected())
                .toList();
        if (selectedDetails.isEmpty()) {
            throw new RuntimeException("Vui lòng chọn ít nhất một sản phẩm cho báo giá");
        }

        for (int index = 0; index < selectedDetails.size(); index += 1) {
            QuotationDetailRequest item = selectedDetails.get(index);
            int lineNumber = index + 1;

            if (item == null || item.getProductId() == null) {
                throw new RuntimeException("Vui lòng chọn sản phẩm ở dòng " + lineNumber);
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new RuntimeException("Số lượng ở dòng " + lineNumber + " phải lớn hơn 0");
            }
        }

        return selectedDetails;
    }

    private void validateOpportunityQuotation(QuotationRequest request, String username) {
        if (request.getOpportunityId() == null) {
            return;
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản nhân viên"));
        var opportunity = opportunityRepository.findDetailById(request.getOpportunityId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ hội bán hàng"));

        if (!canManageOpportunity(user)
                && (opportunity.getAssignedTo() == null
                || !username.equals(opportunity.getAssignedTo().getUsername()))) {
            throw new RuntimeException("Bạn không có quyền tạo báo giá cho cơ hội này");
        }
        if (opportunity.getStage() == null
                || !QUOTATION_STAGES.contains(opportunity.getStage().toUpperCase(Locale.ROOT))) {
            throw new RuntimeException("Giai đoạn cơ hội chưa đủ điều kiện để tạo báo giá");
        }
        if (quotationRepository.findFirstByOpportunityIdOrderByCreatedAtDesc(request.getOpportunityId()).isPresent()) {
            throw new RuntimeException("Cơ hội này đã có báo giá");
        }
        if (request.getCustomerId() != null
                && opportunity.getCustomer() != null
                && !request.getCustomerId().equals(opportunity.getCustomer().getId())) {
            throw new RuntimeException("Khách hàng trong báo giá không khớp với khách hàng của cơ hội");
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
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo giá"));

        return mapToResponse(quotation);
    }

    @Override
    @Transactional
    public void acceptQuotation(Long id, String username) {
        Quotation quotation = quotationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo giá"));

        if (quotation.getStatus() == null || !"DRAFT".equalsIgnoreCase(quotation.getStatus())) {
            throw new RuntimeException("Chỉ có thể duyệt báo giá ở trạng thái bản nháp");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản nhân viên"));
        Long employeeId = user.getEmployee() != null ? user.getEmployee().getId() : null;
        if (!canManageOpportunity(user)
                && (employeeId == null || !employeeId.equals(quotation.getEmployeeId()))) {
            throw new RuntimeException("Bạn không có quyền duyệt báo giá này");
        }


        quotation.setApprovedBy(username);

        quotation.setStatus("APPROVED");

        quotation.setStatus("APPROVED");
        quotationRepository.save(quotation);
    }

    @Override
    public QuotationResponse previewQuotation(QuotationRequest request) {

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

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

        for (QuotationDetailRequest item : requireValidDetails(request.getDetails())) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new RuntimeException("Sản phẩm đã ngừng hoạt động, không thể thêm vào báo giá");
            }

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
            detailResponse.setImageUrl(product.getImageUrl());
            detailResponses.add(detailResponse);
            subTotal = subTotal.add(totalPrice);
        }

        BigDecimal discountAmount = calculateDiscountAmount(request.getVoucherId(), subTotal);

        response.setDetails(detailResponses);
        response.setSubTotal(subTotal);
        response.setDiscountAmount(discountAmount);
        response.setFinalAmount(subTotal.subtract(discountAmount));
        response.setVoucherId(request.getVoucherId());
        if (request.getVoucherId() != null) {
            voucherRepository.findById(request.getVoucherId()).ifPresent(voucher -> {
                response.setVoucherCode(voucher.getVoucherCode());
                response.setVoucherDiscountPercent(voucher.getDiscountPercent());
                response.setVoucherMaxDiscountAmount(voucher.getMaxDiscountAmount());
            });
        }

        return response;
    }

    private BigDecimal calculateDiscountAmount(Long voucherId, BigDecimal subTotal) {
        if (voucherId == null) {
            return BigDecimal.ZERO;
        }

        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher"));
        if (!Boolean.TRUE.equals(voucher.getActive())) {
            throw new RuntimeException("Voucher đã ngừng hoạt động");
        }
        if (voucher.getExpiredAt() != null && !voucher.getExpiredAt().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Voucher đã hết hạn");
        }

        BigDecimal discountPercent = defaultMoney(voucher.getDiscountPercent());

        BigDecimal discountAmount = subTotal
                .multiply(discountPercent)
                .divide(BigDecimal.valueOf(100));

        BigDecimal maxDiscountAmount = defaultMoney(voucher.getMaxDiscountAmount());
        if (maxDiscountAmount.compareTo(BigDecimal.ZERO) > 0
                && discountAmount.compareTo(maxDiscountAmount) > 0) {
            discountAmount = maxDiscountAmount;
        }
        if (discountAmount.compareTo(subTotal) > 0) {
            discountAmount = subTotal;
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

        response.setVoucherId(quotation.getVoucherId());
        if (quotation.getVoucherId() != null) {
            voucherRepository.findById(quotation.getVoucherId()).ifPresent(voucher -> {
                response.setVoucherCode(voucher.getVoucherCode());
                response.setVoucherDiscountPercent(voucher.getDiscountPercent());
                response.setVoucherMaxDiscountAmount(voucher.getMaxDiscountAmount());
            });
        }
        response.setStatus(quotation.getStatus());
        response.setNote(quotation.getNote());
        response.setCreatedAt(quotation.getCreatedAt());
        response.setOpportunityId(quotation.getOpportunityId());
        response.setApprovedBy(quotation.getApprovedBy());
        response.setValidUntil(quotation.getValidUntil());
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
                    d.setImageUrl(product.getImageUrl());
                    return d;
                })
                .toList();

        response.setDetails(details);
        applyDisplayTotals(response, quotation);

        return response;
    }

    private void applyDisplayTotals(QuotationResponse response, Quotation quotation) {
        BigDecimal subTotal = response.getDetails() == null || response.getDetails().isEmpty()
                ? defaultMoney(quotation.getSubTotal())
                : response.getDetails().stream()
                        .map(QuotationDetailResponse::getTotalPrice)
                        .map(this::defaultMoney)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountAmount = defaultMoney(quotation.getDiscountAmount());
        if (discountAmount.compareTo(subTotal) > 0) {
            discountAmount = subTotal;
        }

        response.setSubTotal(subTotal);
        response.setDiscountAmount(discountAmount);
        response.setFinalAmount(subTotal.subtract(discountAmount));
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
