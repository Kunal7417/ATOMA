package com.atoma.marketplace.admin.service;

import com.atoma.marketplace.admin.entity.PlatformPolicy;
import com.atoma.marketplace.admin.repository.PlatformPolicyRepository;
import com.atoma.marketplace.common.enums.DisputeStatus;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.compliance.entity.AuditLog;
import com.atoma.marketplace.compliance.entity.Dispute;
import com.atoma.marketplace.compliance.repository.AuditLogRepository;
import com.atoma.marketplace.compliance.repository.DisputeRepository;
import com.atoma.marketplace.compliance.service.AuditLogService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final PlatformPolicyRepository policyRepository;
    private final DisputeRepository disputeRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;

    public record PolicyRequest(@NotBlank String policyKey, @NotBlank String policyValue, String description) {}

    @Transactional
    public PlatformPolicy upsertPolicy(PolicyRequest request) {
        var existing = policyRepository.findByPolicyKeyAndActiveTrue(request.policyKey());
        if (existing.isPresent()) {
            var policy = existing.get();
            policy.setActive(false);
            policyRepository.save(policy);
        }

        var policy = PlatformPolicy.builder()
                .policyKey(request.policyKey())
                .policyValue(request.policyValue())
                .description(request.description())
                .version(existing.map(p -> p.getVersion() + 1).orElse(1))
                .build();

        auditLogService.log("UPSERT_POLICY", "PlatformPolicy", policy.getPolicyKey(), request.policyValue());
        return policyRepository.save(policy);
    }

    @Transactional(readOnly = true)
    public java.util.List<PlatformPolicy> listPolicies() {
        return policyRepository.findAll();
    }

    @Transactional
    public Dispute resolveDispute(UUID disputeId, DisputeStatus status, String resolutionNotes) {
        var dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> MarketplaceException.notFound("Dispute", disputeId));
        dispute.setStatus(status);
        dispute.setResolutionNotes(resolutionNotes);
        auditLogService.log("RESOLVE_DISPUTE", "Dispute", disputeId.toString(), resolutionNotes);
        return disputeRepository.save(dispute);
    }

    @Transactional(readOnly = true)
    public Page<Dispute> listDisputes(DisputeStatus status, Pageable pageable) {
        if (status == null) {
            return disputeRepository.findAll(pageable);
        }
        return disputeRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> listAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

}
