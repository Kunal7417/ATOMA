package com.atoma.marketplace.order.repository;

import com.atoma.marketplace.order.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findByCustomerId(UUID customerId);

    Optional<WishlistItem> findByCustomerIdAndProductId(UUID customerId, UUID productId);

    boolean existsByCustomerIdAndProductId(UUID customerId, UUID productId);
}
