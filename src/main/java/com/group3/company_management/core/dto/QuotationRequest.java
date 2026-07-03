package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class QuotationRequest {

    private Long customerId;
    private Long opportunityId;
    private Long voucherId;
    private String note;
    private LocalDate quotationDate;
    private LocalDate validUntil;
    private String status;
    private String quotationCode;
    private BigDecimal discountAmount;

    private Long employeeId;
    private List<QuotationDetailRequest> details;
}