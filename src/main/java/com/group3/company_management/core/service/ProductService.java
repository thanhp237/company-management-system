package com.group3.company_management.core.service;

import com.group3.company_management.core.dto.ProductRequest;
import com.group3.company_management.core.dto.ProductResponse;
import org.springframework.data.domain.Page;

public interface ProductService {
    Page<ProductResponse> getProductsPage(int page, int size);

    Page<ProductResponse> searchPage(String keyword, String category, String status, int page, int size);

    ProductResponse getProductById(Long id);

    void createProduct(ProductRequest request);

    void updateProduct(ProductRequest request);

    void updateProductStatus(ProductRequest request);

    void deleteProduct(Long id);
}
