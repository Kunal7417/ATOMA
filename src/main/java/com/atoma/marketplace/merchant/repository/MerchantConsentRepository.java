package com.atoma.marketplace.merchant.repository;

import com.atoma.marketplace.merchant.entity.MerchantConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MerchantConsentRepository extends JpaRepository<MerchantConsent, UUID> {

    List<MerchantConsent> findByMerchantId(UUID merchantId);
}
