package com.group3.company_management.core.dto;

import com.group3.company_management.core.entity.Product;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ProductResponse {
    private Long id;
    private String productCode;
    private String name;
    private String category;
    private String description;
    private BigDecimal unitPrice;
    private Boolean active;
    private LocalDateTime createdAt;

    public static ProductResponse fromEntity(Product product) {
        ProductResponse response = new ProductResponse();
        response.setId(product.getId());
        response.setProductCode(product.getProductCode());
        response.setName(product.getName());
        response.setCategory(product.getCategory());
        response.setDescription(product.getDescription());
        response.setUnitPrice(product.getUnitPrice());
        response.setActive(product.getActive());
        response.setCreatedAt(product.getCreatedAt());
        return response;
    }
}
