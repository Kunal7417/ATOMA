package com.atoma.marketplace.onboarding.repository;

import com.atoma.marketplace.onboarding.entity.OnboardingUploadedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OnboardingUploadedDocumentRepository extends JpaRepository<OnboardingUploadedDocument, UUID> {

    List<OnboardingUploadedDocument> findByMerchantIdOrderByCreatedAtAsc(UUID merchantId);

    long countByMerchantId(UUID merchantId);

    boolean existsByIdAndMerchantId(UUID id, UUID merchantId);
}
