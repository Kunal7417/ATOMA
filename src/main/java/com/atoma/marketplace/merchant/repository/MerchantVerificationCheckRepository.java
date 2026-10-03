package com.atoma.marketplace.merchant.repository;

import com.atoma.marketplace.merchant.entity.MerchantVerificationCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MerchantVerificationCheckRepository extends JpaRepository<MerchantVerificationCheck, UUID> {

    List<MerchantVerificationCheck> findByMerchantId(UUID merchantId);
}
