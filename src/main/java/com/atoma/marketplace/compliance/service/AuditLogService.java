package com.atoma.marketplace.compliance.service;

import com.atoma.marketplace.auth.repository.UserRepository;
import com.atoma.marketplace.auth.security.SecurityUtils;
import com.atoma.marketplace.compliance.entity.AuditLog;
import com.atoma.marketplace.compliance.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;

    @Transactional
    public void log(String action, String entityType, String entityId, String details) {
        var userId = securityUtils.getCurrentUserId();
        var actor = userId != null ? userRepository.getReferenceById(userId) : null;
        auditLogRepository.save(AuditLog.builder()
                .actor(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build());
    }

    @Transactional
    public void logAs(UUID actorUserId, String action, String entityType, String entityId, String details) {
        var actor = actorUserId != null ? userRepository.getReferenceById(actorUserId) : null;
        auditLogRepository.save(AuditLog.builder()
                .actor(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .build());
    }
}
