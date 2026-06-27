package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateContractFromQuotationRequest {

    private Long quotationId;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;
}