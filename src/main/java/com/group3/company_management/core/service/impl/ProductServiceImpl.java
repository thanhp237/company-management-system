package com.group3.company_management.core.service.impl;

import com.group3.company_management.core.dto.ProductRequest;
import com.group3.company_management.core.dto.ProductResponse;
import com.group3.company_management.core.entity.Product;
import com.group3.company_management.core.repository.ProductRepository;
import com.group3.company_management.core.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final List<String> PRODUCT_CATEGORIES = List.of(
            "Camera Body",
            "Lens",
            "Video Camera",
            "Action Camera",
            "Combo Kit"
    );

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsPage(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by("id").ascending());
        return productRepository.findAll(pageable).map(ProductResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchPage(String keyword, String category, String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), size, Sort.by("id").ascending());
        return productRepository.searchProducts(
                normalizeOptional(keyword),
                normalizeOptional(category),
                parseStatus(status),
                pageable
        ).map(ProductResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return ProductResponse.fromEntity(findProductById(id));
    }

    @Override
    public void createProduct(ProductRequest request) {
        String productCode = normalizeRequired(request.getProductCode(), "Vui lòng nhập mã sản phẩm")
                .toUpperCase(Locale.ROOT);
        String name = normalizeRequired(request.getName(), "Vui lòng nhập tên sản phẩm");
        String category = normalizeCategory(request.getCategory());
        BigDecimal unitPrice = normalizePrice(request.getUnitPrice());

        if (productRepository.existsByProductCodeIgnoreCase(productCode)) {
            throw new IllegalArgumentException("Mã sản phẩm đã tồn tại");
        }

        Product product = Product.builder()
                .productCode(productCode)
                .name(name)
                .category(category)
                .description(normalizeOptional(request.getDescription()))
                .unitPrice(unitPrice)
                .active(request.getActive() == null || request.getActive())
                .build();

        productRepository.save(product);
    }

    @Override
    public void updateProduct(ProductRequest request) {
        Long id = normalizeRequired(request.getId(), "Thiếu mã sản phẩm");
        Product product = findProductById(id);
        String productCode = normalizeRequired(request.getProductCode(), "Vui lòng nhập mã sản phẩm")
                .toUpperCase(Locale.ROOT);
        String name = normalizeRequired(request.getName(), "Vui lòng nhập tên sản phẩm");
        String category = normalizeCategory(request.getCategory());
        BigDecimal unitPrice = normalizePrice(request.getUnitPrice());

        if (productRepository.existsByProductCodeIgnoreCaseAndIdNot(productCode, id)) {
            throw new IllegalArgumentException("Mã sản phẩm đã tồn tại");
        }

        product.setProductCode(productCode);
        product.setName(name);
        product.setCategory(category);
        product.setDescription(normalizeOptional(request.getDescription()));
        product.setUnitPrice(unitPrice);
        product.setActive(request.getActive() == null || request.getActive());
        productRepository.save(product);
    }

    @Override
    public void updateProductStatus(ProductRequest request) {
        Long id = normalizeRequired(request.getId(), "Thiếu mã sản phẩm");
        Product product = findProductById(id);
        product.setActive(request.getActive() != null && request.getActive());
        productRepository.save(product);
    }

    @Override
    public void deleteProduct(Long id) {
        Product product = findProductById(id);
        product.setActive(false);
        productRepository.save(product);
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm có mã: " + id));
    }

    private Boolean parseStatus(String status) {
        String normalized = normalizeOptional(status);
        if (normalized == null) {
            return null;
        }
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "ACTIVE" -> true;
            case "INACTIVE" -> false;
            default -> null;
        };
    }

    private String normalizeCategory(String category) {
        String normalized = normalizeRequired(category, "Vui lòng chọn danh mục sản phẩm");
        return PRODUCT_CATEGORIES.stream()
                .filter(item -> item.equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Danh mục sản phẩm không hợp lệ"));
    }

    private BigDecimal normalizePrice(BigDecimal unitPrice) {
        if (unitPrice == null) {
            throw new IllegalArgumentException("Vui lòng nhập đơn giá");
        }
        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Đơn giá phải lớn hơn hoặc bằng 0");
        }
        return unitPrice;
    }

    private String normalizeOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private <T> T normalizeRequired(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
