package com.atoma.marketplace.merchant.repository;

import com.atoma.marketplace.merchant.entity.MerchantApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MerchantApplicationStatusHistoryRepository extends JpaRepository<MerchantApplicationStatusHistory, UUID> {

    List<MerchantApplicationStatusHistory> findByMerchantIdOrderByOccurredAtAsc(UUID merchantId);
}
