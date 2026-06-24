package com.group3.company_management.core.repository;

import com.group3.company_management.core.entity.Product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrue();
    boolean existsByProductCodeIgnoreCase(String productCode);

    boolean existsByProductCodeIgnoreCaseAndIdNot(String productCode, Long id);

    @Query("""
            SELECT p FROM Product p
            WHERE (:keyword IS NULL OR :keyword = ''
                OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(p.category) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:category IS NULL OR :category = '' OR LOWER(p.category) = LOWER(:category))
            AND (:active IS NULL OR p.active = :active)
            """)
    Page<Product> searchProducts(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("active") Boolean active,
            Pageable pageable
    );
}

