package com.atoma.marketplace.product.repository;

import com.atoma.marketplace.product.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findByProductIdAndApprovedTrue(UUID productId, Pageable pageable);
}
