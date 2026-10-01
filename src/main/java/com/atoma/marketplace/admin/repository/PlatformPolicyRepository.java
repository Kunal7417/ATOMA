package com.atoma.marketplace.admin.repository;

import com.atoma.marketplace.admin.entity.PlatformPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlatformPolicyRepository extends JpaRepository<PlatformPolicy, UUID> {

    Optional<PlatformPolicy> findByPolicyKeyAndActiveTrue(String policyKey);
}
