package com.group3.company_management.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemFormRow {
    private Long quotationDetailId;
    private String serviceName;
    private String imageUrl;
    private String description;
    private Integer contractQuantity;
    private Integer invoicedQuantity;
    private Integer remainingQuantity;
    private BigDecimal unitPrice;

    private Integer quantity;
    private boolean selected;
}
