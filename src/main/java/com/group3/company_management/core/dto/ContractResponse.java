package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class ContractResponse {

    private Long id;

    private String contractCode;

    private String status;

    private Long quotationId;

    private String quotationCode;

    private Long customerId;

    private String customerName;

    private String customerEmail;

    private String customerPhone;

    private String customerAddress;

    private BigDecimal contractAmount;

    private BigDecimal discountAmount;

    private BigDecimal finalAmount;

    private Long saleId;

    private String saleName;

    private Long adminOfficerId;

    private String adminOfficerName;

    private LocalDateTime createdAt;

    private LocalDateTime signedAt;

    private LocalDate contractStartDate;

    private LocalDate contractEndDate;

    private String paymentTerms;

    private String deliveryTerms;

    private String legalTerms;

    private String adminNote;

    private List<ContractItemResponse> items;
}
