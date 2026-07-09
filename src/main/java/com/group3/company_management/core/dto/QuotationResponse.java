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

    private String status;

    private String note;

    private LocalDateTime createdAt;

    private List<QuotationDetailResponse> details;
    private Long opportunityId;

}