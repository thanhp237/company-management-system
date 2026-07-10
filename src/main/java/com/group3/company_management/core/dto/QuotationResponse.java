package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class QuotationResponse {

    private Long id;

    private String quotationCode;

    private Long customerId;

    private String customerName;

    private String customerEmail;

    private String customerPhone;

    private String customerAddress;

    private BigDecimal subTotal;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private Long voucherId;

    private String voucherCode;

    private BigDecimal voucherDiscountPercent;

    private BigDecimal voucherMaxDiscountAmount;

    private String status;

    private String note;
    private java.time.LocalDate validUntil;
    private LocalDateTime createdAt;
    private String approvedBy;
    private List<QuotationDetailResponse> details;
    private Long opportunityId;
}
