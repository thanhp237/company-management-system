package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class QuotationDetailResponse {

    private Long productId;

    private String productCode;

    private String productName;

    private String description;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;
}
