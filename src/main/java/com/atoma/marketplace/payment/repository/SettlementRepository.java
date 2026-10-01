package com.atoma.marketplace.payment.repository;

import com.atoma.marketplace.payment.entity.Settlement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    Page<Settlement> findByMerchantId(UUID merchantId, Pageable pageable);

    Optional<Settlement> findByOrderId(UUID orderId);
}
