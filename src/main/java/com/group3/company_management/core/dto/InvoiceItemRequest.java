package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class InvoiceItemRequest {
    private Long quotationDetailId;
    private Integer quantity;
}