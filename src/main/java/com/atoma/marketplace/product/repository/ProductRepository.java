package com.atoma.marketplace.product.repository;

import com.atoma.marketplace.common.enums.ProductStatus;
import com.atoma.marketplace.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    Page<Product> findByMerchantId(UUID merchantId, Pageable pageable);

    Page<Product> findByMerchantIdAndStatus(UUID merchantId, ProductStatus status, Pageable pageable);

    Page<Product> findByCategoryIdAndStatus(UUID categoryId, ProductStatus status, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.status = :status
            AND (
                LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%'))
            )
            """)
    Page<Product> searchActiveProducts(@Param("query") String query,
                                       @Param("status") ProductStatus status,
                                       Pageable pageable);
}
