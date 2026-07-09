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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {
    private static final Path PRODUCT_UPLOAD_DIR = Paths.get("uploads", "products");
    private static final String PRODUCT_UPLOAD_URL_PREFIX = "/uploads/products/";
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

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
                .imageUrl(storeProductImage(request.getImageFile(), null))
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
        product.setImageUrl(storeProductImage(request.getImageFile(), product.getImageUrl()));
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

    private String storeProductImage(MultipartFile imageFile, String currentImageUrl) {
        if (imageFile == null || imageFile.isEmpty()) {
            return currentImageUrl;
        }

        validateImageFile(imageFile);

        String extension = getFileExtension(imageFile.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try {
            Files.createDirectories(PRODUCT_UPLOAD_DIR);
            Path destination = PRODUCT_UPLOAD_DIR.resolve(filename).toAbsolutePath().normalize();
            Path uploadRoot = PRODUCT_UPLOAD_DIR.toAbsolutePath().normalize();
            if (!destination.startsWith(uploadRoot)) {
                throw new IllegalArgumentException("Tên file ảnh không hợp lệ");
            }
            Files.copy(imageFile.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            deleteOldImage(currentImageUrl);
            return PRODUCT_UPLOAD_URL_PREFIX + filename;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Không thể lưu ảnh sản phẩm");
        }
    }

    private void validateImageFile(MultipartFile imageFile) {
        if (imageFile.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Ảnh sản phẩm không được vượt quá 5MB");
        }

        String extension = getFileExtension(imageFile.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Ảnh sản phẩm chỉ hỗ trợ JPG, PNG, WEBP hoặc GIF");
        }

        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("File tải lên phải là ảnh");
        }
    }

    private String getFileExtension(String originalFilename) {
        String filename = normalizeOptional(originalFilename);
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("Ảnh sản phẩm phải có phần mở rộng file");
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private void deleteOldImage(String currentImageUrl) {
        String normalized = normalizeOptional(currentImageUrl);
        if (normalized == null || !normalized.startsWith(PRODUCT_UPLOAD_URL_PREFIX)) {
            return;
        }

        try {
            String filename = normalized.substring(PRODUCT_UPLOAD_URL_PREFIX.length());
            Path uploadRoot = PRODUCT_UPLOAD_DIR.toAbsolutePath().normalize();
            Path oldImage = PRODUCT_UPLOAD_DIR.resolve(filename).toAbsolutePath().normalize();
            if (oldImage.startsWith(uploadRoot)) {
                Files.deleteIfExists(oldImage);
            }
        } catch (IOException ignored) {
            // The database has already moved on; a stale file should not block product updates.
        }
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
