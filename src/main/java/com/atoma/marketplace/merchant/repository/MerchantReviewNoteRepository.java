package com.atoma.marketplace.merchant.repository;

import com.atoma.marketplace.merchant.entity.MerchantReviewNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MerchantReviewNoteRepository extends JpaRepository<MerchantReviewNote, UUID> {

    List<MerchantReviewNote> findByMerchantIdAndResolvedFalseOrderByCreatedAtAsc(UUID merchantId);

    List<MerchantReviewNote> findByMerchantIdOrderByCreatedAtAsc(UUID merchantId);

    List<MerchantReviewNote> findByMerchantId(UUID merchantId);
}
