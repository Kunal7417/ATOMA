package com.atoma.marketplace.product.repository;

import com.atoma.marketplace.common.enums.ProductStatus;
import com.atoma.marketplace.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
