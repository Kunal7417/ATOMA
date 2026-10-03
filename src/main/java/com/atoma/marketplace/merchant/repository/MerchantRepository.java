package com.atoma.marketplace.merchant.repository;

import com.atoma.marketplace.common.enums.MerchantStatus;
import com.atoma.marketplace.merchant.entity.Merchant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MerchantRepository extends JpaRepository<Merchant, UUID> {

    Optional<Merchant> findByOwnerId(UUID ownerId);

    Page<Merchant> findByStatus(MerchantStatus status, Pageable pageable);

    Optional<Merchant> findByApplicationNumber(String applicationNumber);
}
