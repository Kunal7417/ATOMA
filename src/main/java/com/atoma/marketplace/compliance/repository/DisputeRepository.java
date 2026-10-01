package com.atoma.marketplace.compliance.repository;

import com.atoma.marketplace.common.enums.DisputeStatus;
import com.atoma.marketplace.compliance.entity.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    Page<Dispute> findByStatus(DisputeStatus status, Pageable pageable);

    Page<Dispute> findByCustomerId(UUID customerId, Pageable pageable);

    Page<Dispute> findByMerchantId(UUID merchantId, Pageable pageable);
}
