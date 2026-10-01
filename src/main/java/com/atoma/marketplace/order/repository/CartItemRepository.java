package com.atoma.marketplace.order.repository;

import com.atoma.marketplace.order.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCustomerId(UUID customerId);

    Optional<CartItem> findByCustomerIdAndProductId(UUID customerId, UUID productId);

    void deleteByCustomerId(UUID customerId);
}
