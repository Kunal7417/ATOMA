package com.atoma.marketplace.merchant.repository;

import com.atoma.marketplace.merchant.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface KycDocumentRepository extends JpaRepository<KycDocument, UUID> {

    List<KycDocument> findByMerchantId(UUID merchantId);
}
