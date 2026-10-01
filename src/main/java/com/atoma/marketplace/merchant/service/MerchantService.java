package com.atoma.marketplace.merchant.service;

import com.atoma.marketplace.auth.repository.UserRepository;
import com.atoma.marketplace.auth.security.SecurityUtils;
import com.atoma.marketplace.common.enums.KycDocumentType;
import com.atoma.marketplace.common.enums.KycStatus;
import com.atoma.marketplace.common.enums.MerchantStatus;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.compliance.service.AuditLogService;
import com.atoma.marketplace.merchant.dto.MerchantDtos;
import com.atoma.marketplace.merchant.entity.KycDocument;
import com.atoma.marketplace.merchant.entity.Merchant;
import com.atoma.marketplace.merchant.repository.KycDocumentRepository;
import com.atoma.marketplace.merchant.repository.MerchantRepository;
import com.atoma.marketplace.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public MerchantDtos.MerchantResponse onboard(MerchantDtos.MerchantOnboardRequest request) {
        var userId = requireCurrentUserId();
        if (merchantRepository.findByOwnerId(userId).isPresent()) {
            throw MarketplaceException.conflict("Merchant profile already exists");
        }

        var owner = userRepository.findById(userId)
                .orElseThrow(() -> MarketplaceException.notFound("User", userId));

        var merchant = Merchant.builder()
                .owner(owner)
                .businessName(request.businessName())
                .description(request.description())
                .tinNumber(request.tinNumber())
                .businessLicenseNumber(request.businessLicenseNumber())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .address(request.address())
                .city(request.city())
                .status(MerchantStatus.PENDING_KYC)
                .kycStatus(KycStatus.NOT_STARTED)
                .build();

        return toResponse(merchantRepository.save(merchant));
    }

    @Transactional
    public MerchantDtos.MerchantResponse uploadKycDocument(MerchantDtos.KycDocumentRequest request) {
        var merchant = getOwnedMerchant();
        if (merchant.getStatus() == MerchantStatus.VERIFIED) {
            throw MarketplaceException.badRequest("KYC is already approved");
        }

        KycDocumentType documentType;
        try {
            documentType = KycDocumentType.fromString(request.documentType());
        } catch (IllegalArgumentException ex) {
            throw MarketplaceException.badRequest(ex.getMessage());
        }

        var document = KycDocument.builder()
                .merchant(merchant)
                .documentType(documentType.name())
                .fileUrl(request.fileUrl())
                .reviewStatus(KycStatus.SUBMITTED)
                .build();
        kycDocumentRepository.save(document);

        merchant.setKycStatus(KycStatus.SUBMITTED);
        merchant.setStatus(MerchantStatus.PENDING_KYC);
        return toResponse(merchantRepository.save(merchant));
    }

    @Transactional(readOnly = true)
    public java.util.List<MerchantDtos.KycDocumentResponse> listKycDocuments() {
        var merchant = getOwnedMerchant();
        return kycDocumentRepository.findByMerchantId(merchant.getId()).stream()
                .map(this::toKycResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MerchantDtos.MerchantResponse getMyMerchantProfile() {
        return toResponse(getOwnedMerchant());
    }

    @Transactional(readOnly = true)
    public Page<MerchantDtos.MerchantResponse> listByStatus(MerchantStatus status, Pageable pageable) {
        return merchantRepository.findByStatus(status, pageable).map(this::toResponse);
    }

    @Transactional
    public MerchantDtos.MerchantResponse verifyMerchant(UUID merchantId, boolean approved, String notes) {
        var merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> MarketplaceException.notFound("Merchant", merchantId));

        if (approved && !hasRequiredKycDocuments(merchant.getId())) {
            throw MarketplaceException.badRequest(
                    "Merchant has not submitted all required KYC documents: "
                            + KycDocumentType.REQUIRED_FOR_SUBMISSION);
        }

        var kycReviewStatus = approved ? KycStatus.APPROVED : KycStatus.REJECTED;
        for (var document : kycDocumentRepository.findByMerchantId(merchant.getId())) {
            document.setReviewStatus(kycReviewStatus);
            document.setReviewNotes(notes);
            kycDocumentRepository.save(document);
        }

        if (approved) {
            merchant.setStatus(MerchantStatus.VERIFIED);
            merchant.setKycStatus(KycStatus.APPROVED);
            merchant.setProvisionalActive(true);
            notificationService.notifyKycApproved(merchant);
        } else {
            merchant.setStatus(MerchantStatus.REJECTED);
            merchant.setKycStatus(KycStatus.REJECTED);
            merchant.setProvisionalActive(false);
            notificationService.notifyKycRejected(merchant, notes);
        }

        auditLogService.log(
                approved ? "VERIFY_MERCHANT" : "REJECT_MERCHANT",
                "Merchant",
                merchantId.toString(),
                notes != null ? notes : (approved ? "approved" : "rejected"));

        return toResponse(merchantRepository.save(merchant));
    }

    public void assertCanManageCatalog() {
        var merchant = getOwnedMerchant();
        if (merchant.getStatus() != MerchantStatus.VERIFIED && !merchant.isProvisionalActive()) {
            throw MarketplaceException.forbidden(
                    "Merchant must complete KYC verification before managing product listings");
        }
    }

    public Merchant getOwnedMerchant() {
        var userId = requireCurrentUserId();
        return merchantRepository.findByOwnerId(userId)
                .orElseThrow(() -> MarketplaceException.notFound("Merchant for user", userId));
    }

    public Merchant getById(UUID id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> MarketplaceException.notFound("Merchant", id));
    }

    private boolean hasRequiredKycDocuments(UUID merchantId) {
        var uploaded = kycDocumentRepository.findByMerchantId(merchantId).stream()
                .map(KycDocument::getDocumentType)
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toSet());
        return uploaded.containsAll(KycDocumentType.REQUIRED_FOR_SUBMISSION.stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toSet()));
    }

    private UUID requireCurrentUserId() {
        var userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw MarketplaceException.unauthorized("Authentication required");
        }
        return userId;
    }

    private MerchantDtos.KycDocumentResponse toKycResponse(KycDocument document) {
        return MerchantDtos.KycDocumentResponse.builder()
                .id(document.getId())
                .documentType(document.getDocumentType())
                .fileUrl(document.getFileUrl())
                .reviewStatus(document.getReviewStatus())
                .reviewNotes(document.getReviewNotes())
                .build();
    }

    private MerchantDtos.MerchantResponse toResponse(Merchant merchant) {
        return MerchantDtos.MerchantResponse.builder()
                .id(merchant.getId())
                .ownerUserId(merchant.getOwner().getId())
                .businessName(merchant.getBusinessName())
                .description(merchant.getDescription())
                .status(merchant.getStatus())
                .kycStatus(merchant.getKycStatus())
                .address(merchant.getAddress())
                .city(merchant.getCity())
                .commissionRate(merchant.getCommissionRate())
                .provisionalActive(merchant.isProvisionalActive())
                .build();
    }
}
