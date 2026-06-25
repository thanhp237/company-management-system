package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class QuotationRequest {

    private Long customerId;
    private Long opportunityId;
    private Long voucherId;
    private String note;

    private List<QuotationDetailRequest> details;
}