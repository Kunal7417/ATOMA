package com.atoma.marketplace.order.repository;

import com.atoma.marketplace.common.enums.OrderStatus;
import com.atoma.marketplace.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Order> findByMerchantId(UUID merchantId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}
