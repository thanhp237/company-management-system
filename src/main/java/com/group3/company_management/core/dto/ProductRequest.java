package com.group3.company_management.core.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {
    private Long id;
    private String productCode;
    private String name;
    private String category;
    private String description;
    private BigDecimal unitPrice;
    private Boolean active;
}
