package com.atoma.marketplace.merchant.service;

import com.atoma.marketplace.auth.entity.User;
import com.atoma.marketplace.auth.repository.UserRepository;
import com.atoma.marketplace.auth.security.SecurityUtils;
import com.atoma.marketplace.common.enums.ApplicationWorkflowStatus;
import com.atoma.marketplace.common.enums.KycDocumentType;
import com.atoma.marketplace.common.enums.KycStatus;
import com.atoma.marketplace.common.enums.MerchantStatus;
import com.atoma.marketplace.common.enums.PayoutMethod;
import com.atoma.marketplace.common.enums.VerificationCheckStatus;
import com.atoma.marketplace.common.enums.VerificationCheckType;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.compliance.service.AuditLogService;
import com.atoma.marketplace.merchant.dto.OnboardingDtos;
import com.atoma.marketplace.merchant.entity.KycDocument;
import com.atoma.marketplace.merchant.entity.Merchant;
import com.atoma.marketplace.merchant.entity.MerchantApplicationStatusHistory;
import com.atoma.marketplace.merchant.entity.MerchantConsent;
import com.atoma.marketplace.merchant.entity.MerchantReviewNote;
import com.atoma.marketplace.merchant.entity.MerchantVerificationCheck;
import com.atoma.marketplace.merchant.repository.KycDocumentRepository;
import com.atoma.marketplace.merchant.repository.MerchantApplicationStatusHistoryRepository;
import com.atoma.marketplace.merchant.repository.MerchantConsentRepository;
import com.atoma.marketplace.merchant.repository.MerchantRepository;
import com.atoma.marketplace.merchant.repository.MerchantReviewNoteRepository;
import com.atoma.marketplace.merchant.repository.MerchantVerificationCheckRepository;
import com.atoma.marketplace.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantOnboardingService {

    private static final Set<String> REQUIRED_CONSENTS = Set.of(
            "MERCHANT_AGREEMENT",
            "DATA_ACCURACY",
            "VERIFICATION_CONSENT"
    );

    private final MerchantRepository merchantRepository;
    private final KycDocumentRepository kycDocumentRepository;
    private final MerchantApplicationStatusHistoryRepository historyRepository;
    private final MerchantReviewNoteRepository reviewNoteRepository;
    private final MerchantVerificationCheckRepository verificationCheckRepository;
    private final MerchantConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final ApplicationNumberGenerator applicationNumberGenerator;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public OnboardingDtos.WizardProgressResponse getProgress() {
        var merchant = findOrNullMerchant();
        if (merchant == null) {
            return OnboardingDtos.WizardProgressResponse.builder()
                    .currentWizardStep(0)
                    .workflowStatus(ApplicationWorkflowStatus.DRAFT)
                    .readyToSubmit(false)
                    .build();
        }
        return toProgress(merchant);
    }

    @Transactional
    public OnboardingDtos.WizardProgressResponse saveWizardStep(OnboardingDtos.WizardStepRequest request) {
        var merchant = getOrCreateDraftMerchant();
        assertEditable(merchant);

        switch (request.step()) {
            case 1 -> applyBusinessStep(merchant, request.business());
            case 2 -> applyOwnerStep(merchant, request.owner());
            case 3 -> applyDocumentsStep(merchant, request.documents());
            case 4 -> applyStoreAddressStep(merchant, request.storeAddress());
            case 5 -> applyPayoutStep(merchant, request.payout());
            case 6 -> applyTermsStep(merchant, request.terms());
            default -> throw MarketplaceException.badRequest("Step 7 is review only; use submit endpoint");
        }

        merchant.setCurrentWizardStep(Math.max(merchant.getCurrentWizardStep(), request.step()));
        merchant.setDraftSavedAt(Instant.now());
        if (merchant.getApplicationWorkflowStatus() == null) {
            merchant.setApplicationWorkflowStatus(ApplicationWorkflowStatus.DRAFT);
        }
        merchantRepository.save(merchant);
        return toProgress(merchant);
    }

    @Transactional
    public OnboardingDtos.WizardProgressResponse saveDraft() {
        var merchant = getOrCreateDraftMerchant();
        assertEditable(merchant);
        merchant.setDraftSavedAt(Instant.now());
        merchantRepository.save(merchant);
        return toProgress(merchant);
    }

    @Transactional
    public OnboardingDtos.SubmitApplicationResponse submitApplication() {
        var merchant = getOwnedMerchant();
        assertEditable(merchant);
        validateReadyToSubmit(merchant);

        if (merchant.getApplicationNumber() == null) {
            merchant.setApplicationNumber(applicationNumberGenerator.generate());
        }
        merchant.setSubmittedAt(Instant.now());
        merchant.setStatus(MerchantStatus.PENDING_KYC);
        merchant.setKycStatus(KycStatus.SUBMITTED);

        transition(merchant, ApplicationWorkflowStatus.SUBMITTED, "Application submitted");
        transition(merchant, ApplicationWorkflowStatus.UNDER_REVIEW, "Queued for review");
        initializeVerificationChecks(merchant);

        merchantRepository.save(merchant);
        auditLogService.log("SUBMIT_MERCHANT_APPLICATION", "Merchant", merchant.getId().toString(), null);

        return OnboardingDtos.SubmitApplicationResponse.builder()
                .applicationNumber(merchant.getApplicationNumber())
                .workflowStatus(merchant.getApplicationWorkflowStatus())
                .message("We'll notify you when there is a decision — most reviews finish within 24–48 hours.")
                .build();
    }

    @Transactional
    public OnboardingDtos.SubmitApplicationResponse resubmitAfterUpdates() {
        var merchant = getOwnedMerchant();
        if (merchant.getApplicationWorkflowStatus() != ApplicationWorkflowStatus.NEEDS_UPDATE) {
            throw MarketplaceException.badRequest("Application is not awaiting updates");
        }
        validateReadyToSubmit(merchant);

        reviewNoteRepository.findByMerchantId(merchant.getId()).forEach(note -> note.setResolved(true));
        merchant.setFixByDeadline(null);
        transition(merchant, ApplicationWorkflowStatus.UNDER_REVIEW, "Updates resubmitted");
        initializeVerificationChecks(merchant);
        merchantRepository.save(merchant);

        return OnboardingDtos.SubmitApplicationResponse.builder()
                .applicationNumber(merchant.getApplicationNumber())
                .workflowStatus(merchant.getApplicationWorkflowStatus())
                .message("Application sent back for review")
                .build();
    }

    @Transactional(readOnly = true)
    public OnboardingDtos.ApplicationStatusResponse getApplicationStatus() {
        var merchant = getOwnedMerchant();
        return buildStatusResponse(merchant);
    }

    @Transactional
    public OnboardingDtos.ApplicationStatusResponse adminReview(UUID merchantId, OnboardingDtos.AdminApplicationReviewRequest request) {
        var merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> MarketplaceException.notFound("Merchant", merchantId));

        switch (request.action()) {
            case START_REVIEW -> {
                if (merchant.getApplicationWorkflowStatus() == ApplicationWorkflowStatus.SUBMITTED) {
                    transition(merchant, ApplicationWorkflowStatus.UNDER_REVIEW, "Review started");
                }
            }
            case REQUEST_UPDATES -> {
                if (request.reviewerNotes() == null || request.reviewerNotes().isEmpty()) {
                    throw MarketplaceException.badRequest("Reviewer notes are required");
                }
                reviewNoteRepository.findByMerchantId(merchant.getId()).forEach(n -> n.setResolved(true));
                for (var note : request.reviewerNotes()) {
                    reviewNoteRepository.save(MerchantReviewNote.builder()
                            .merchant(merchant)
                            .fieldKey(note.fieldKey())
                            .message(note.message())
                            .resolved(false)
                            .build());
                }
                merchant.setFixByDeadline(request.fixByDeadline());
                transition(merchant, ApplicationWorkflowStatus.NEEDS_UPDATE, "Updates requested");
            }
            case APPROVE -> approveMerchant(merchant);
            case REJECT -> {
                merchant.setStatus(MerchantStatus.REJECTED);
                merchant.setKycStatus(KycStatus.REJECTED);
                merchant.setProvisionalActive(false);
                merchant.setReapplyAfter(request.reapplyAfter());
                merchant.setDecisionReference(request.decisionReference());
                transition(merchant, ApplicationWorkflowStatus.REJECTED,
                        request.rejectionReason() != null ? request.rejectionReason() : "Rejected");
                notificationService.notifyKycRejected(merchant, request.rejectionReason());
            }
        }
        merchantRepository.save(merchant);
        auditLogService.log("ADMIN_REVIEW_" + request.action(), "Merchant", merchantId.toString(), null);
        return buildStatusResponse(merchant);
    }

    @Transactional
    public OnboardingDtos.ApplicationStatusResponse suspendMerchant(UUID merchantId, OnboardingDtos.SuspendMerchantRequest request) {
        var merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> MarketplaceException.notFound("Merchant", merchantId));
        merchant.setStatus(MerchantStatus.SUSPENDED);
        merchant.setSuspensionReason(request.reason());
        transition(merchant, ApplicationWorkflowStatus.SUSPENDED, request.reason());
        merchantRepository.save(merchant);
        return buildStatusResponse(merchant);
    }

    @Transactional
    public OnboardingDtos.ApplicationStatusResponse submitLicenceRenewal(OnboardingDtos.LicenceRenewalRequest request) {
        var merchant = getOwnedMerchant();
        if (merchant.getApplicationWorkflowStatus() != ApplicationWorkflowStatus.SUSPENDED) {
            throw MarketplaceException.badRequest("Licence renewal is only for suspended merchants");
        }
        upsertDocument(merchant, KycDocumentType.BUSINESS_LICENSE, request.fileUrl(),
                request.licenceNumber(), request.expiryDate());
        merchant.setSuspensionReason(null);
        merchant.setStatus(MerchantStatus.PENDING_KYC);
        transition(merchant, ApplicationWorkflowStatus.UNDER_REVIEW, "Renewed licence submitted");
        merchantRepository.save(merchant);
        return buildStatusResponse(merchant);
    }

    private void approveMerchant(Merchant merchant) {
        if (!hasRequiredDocuments(merchant.getId())) {
            throw MarketplaceException.badRequest("Required documents missing");
        }
        for (var document : kycDocumentRepository.findByMerchantId(merchant.getId())) {
            document.setReviewStatus(KycStatus.APPROVED);
            kycDocumentRepository.save(document);
        }
        merchant.setStatus(MerchantStatus.VERIFIED);
        merchant.setKycStatus(KycStatus.APPROVED);
        merchant.setProvisionalActive(true);
        transition(merchant, ApplicationWorkflowStatus.APPROVED, "Approved");
        markAllChecksDone(merchant);
        notificationService.notifyKycApproved(merchant);
    }

    private void applyBusinessStep(Merchant merchant, OnboardingDtos.BusinessStepRequest business) {
        if (business == null) {
            throw MarketplaceException.badRequest("Business step payload required");
        }
        merchant.setBusinessType(business.businessType());
        merchant.setBusinessName(business.businessName());
        merchant.setMainCategory(business.mainCategory());
    }

    private void applyOwnerStep(Merchant merchant, OnboardingDtos.OwnerStepRequest owner) {
        if (owner == null) {
            throw MarketplaceException.badRequest("Owner step payload required");
        }
        merchant.setRepresentativeRole(owner.representativeRole());
        merchant.setOwnerFullName(owner.fullName());
        merchant.setOwnerEmail(owner.email());
        merchant.setPhoneVerified(true);
    }

    private void applyDocumentsStep(Merchant merchant, OnboardingDtos.DocumentsStepRequest documents) {
        if (documents == null || documents.documents().isEmpty()) {
            throw MarketplaceException.badRequest("Documents step payload required");
        }
        for (var doc : documents.documents()) {
            var type = KycDocumentType.fromString(doc.documentType());
            upsertDocument(merchant, type, doc.fileUrl(), doc.documentNumber(), doc.expiryDate());
        }
        if (documents.documents().stream().anyMatch(d -> KycDocumentType.TIN.name().equalsIgnoreCase(d.documentType()))) {
            documents.documents().stream()
                    .filter(d -> KycDocumentType.TIN.name().equalsIgnoreCase(d.documentType()))
                    .findFirst()
                    .ifPresent(d -> {
                        if (d.documentNumber() != null) {
                            merchant.setTinNumber(d.documentNumber());
                        }
                    });
        }
    }

    private void applyStoreAddressStep(Merchant merchant, OnboardingDtos.StoreAddressStepRequest address) {
        if (address == null) {
            throw MarketplaceException.badRequest("Store address step payload required");
        }
        merchant.setBuildingNumber(address.buildingNumber());
        merchant.setStreet(address.street());
        merchant.setDistrict(address.district());
        merchant.setCity(address.city());
        merchant.setLandmark(address.landmark());
        merchant.setLatitude(address.latitude());
        merchant.setLongitude(address.longitude());
        merchant.setAddress(address.buildingNumber() + ", " + address.street());
    }

    private void applyPayoutStep(Merchant merchant, OnboardingDtos.PayoutStepRequest payout) {
        if (payout == null) {
            throw MarketplaceException.badRequest("Payout step payload required");
        }
        merchant.setPayoutMethod(payout.payoutMethod());
        if (payout.payoutMethod() == PayoutMethod.BANK
                && (payout.bankAccountHint() == null || payout.bankAccountHint().isBlank())) {
            throw MarketplaceException.badRequest("Bank account hint required for bank payout");
        }
        merchant.setBankAccountHint(payout.bankAccountHint());
    }

    private void applyTermsStep(Merchant merchant, OnboardingDtos.TermsStepRequest terms) {
        if (terms == null || terms.consents().isEmpty()) {
            throw MarketplaceException.badRequest("Terms step payload required");
        }
        consentRepository.deleteAll(consentRepository.findByMerchantId(merchant.getId()));
        for (var item : terms.consents()) {
            if (!item.accepted()) {
                throw MarketplaceException.badRequest("All consents must be accepted");
            }
            consentRepository.save(MerchantConsent.builder()
                    .merchant(merchant)
                    .consentKey(item.consentKey())
                    .policyVersion(item.policyVersion())
                    .acceptedAt(Instant.now())
                    .build());
        }
        var acceptedKeys = terms.consents().stream()
                .map(OnboardingDtos.ConsentItemRequest::consentKey)
                .collect(java.util.stream.Collectors.toSet());
        if (!acceptedKeys.containsAll(REQUIRED_CONSENTS)) {
            throw MarketplaceException.badRequest("Missing required consents: " + REQUIRED_CONSENTS);
        }
    }

    private void upsertDocument(
            Merchant merchant,
            KycDocumentType type,
            String fileUrl,
            String documentNumber,
            java.time.LocalDate expiryDate
    ) {
        var existing = kycDocumentRepository.findByMerchantId(merchant.getId()).stream()
                .filter(d -> d.getDocumentType().equalsIgnoreCase(type.name()))
                .findFirst();
        if (existing.isPresent()) {
            var doc = existing.get();
            doc.setFileUrl(fileUrl);
            doc.setDocumentNumber(documentNumber);
            doc.setExpiryDate(expiryDate);
            doc.setReviewStatus(KycStatus.SUBMITTED);
            kycDocumentRepository.save(doc);
        } else {
            kycDocumentRepository.save(KycDocument.builder()
                    .merchant(merchant)
                    .documentType(type.name())
                    .fileUrl(fileUrl)
                    .documentNumber(documentNumber)
                    .expiryDate(expiryDate)
                    .reviewStatus(KycStatus.SUBMITTED)
                    .build());
        }
        if (type == KycDocumentType.BUSINESS_LICENSE && documentNumber != null) {
            merchant.setBusinessLicenseNumber(documentNumber);
        }
    }

    private void validateReadyToSubmit(Merchant merchant) {
        if (merchant.getCurrentWizardStep() < 6) {
            throw MarketplaceException.badRequest("Complete all wizard steps before submitting");
        }
        if (merchant.getBusinessType() == null || merchant.getBusinessName() == null || merchant.getMainCategory() == null) {
            throw MarketplaceException.badRequest("Business details incomplete");
        }
        if (merchant.getOwnerFullName() == null || merchant.getRepresentativeRole() == null) {
            throw MarketplaceException.badRequest("Owner details incomplete");
        }
        if (!hasRequiredDocuments(merchant.getId())) {
            throw MarketplaceException.badRequest("All required documents must be uploaded");
        }
        if (merchant.getDistrict() == null || merchant.getStreet() == null || merchant.getLatitude() == null) {
            throw MarketplaceException.badRequest("Store address incomplete");
        }
        if (merchant.getPayoutMethod() == null) {
            throw MarketplaceException.badRequest("Payout method required");
        }
        var consents = consentRepository.findByMerchantId(merchant.getId()).stream()
                .map(MerchantConsent::getConsentKey)
                .collect(java.util.stream.Collectors.toSet());
        if (!consents.containsAll(REQUIRED_CONSENTS)) {
            throw MarketplaceException.badRequest("Terms and consents incomplete");
        }
    }

    private boolean hasRequiredDocuments(UUID merchantId) {
        var uploaded = kycDocumentRepository.findByMerchantId(merchantId).stream()
                .map(KycDocument::getDocumentType)
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toSet());
        return uploaded.containsAll(KycDocumentType.REQUIRED_FOR_SUBMISSION.stream()
                .map(Enum::name)
                .collect(java.util.stream.Collectors.toSet()));
    }

    private void initializeVerificationChecks(Merchant merchant) {
        upsertCheck(merchant, VerificationCheckType.MOBILE_VERIFIED,
                merchant.isPhoneVerified() ? VerificationCheckStatus.DONE : VerificationCheckStatus.WAITING);
        var docCount = kycDocumentRepository.findByMerchantId(merchant.getId()).size();
        var required = KycDocumentType.REQUIRED_FOR_SUBMISSION.size();
        upsertCheck(merchant, VerificationCheckType.DOCUMENTS_RECEIVED,
                docCount >= required ? VerificationCheckStatus.DONE : VerificationCheckStatus.WAITING);
        upsertCheck(merchant, VerificationCheckType.LICENCE_TAZKIRA_CHECK, VerificationCheckStatus.IN_PROGRESS);
        upsertCheck(merchant, VerificationCheckType.PAYOUT_ACCOUNT_CHECK, VerificationCheckStatus.WAITING);
    }

    private void markAllChecksDone(Merchant merchant) {
        for (var type : VerificationCheckType.values()) {
            upsertCheck(merchant, type, VerificationCheckStatus.DONE);
        }
    }

    private void upsertCheck(Merchant merchant, VerificationCheckType type, VerificationCheckStatus status) {
        var checks = verificationCheckRepository.findByMerchantId(merchant.getId());
        var existing = checks.stream().filter(c -> c.getCheckType() == type).findFirst();
        if (existing.isPresent()) {
            var check = existing.get();
            check.setStatus(status);
            check.setLastCheckedAt(Instant.now());
            verificationCheckRepository.save(check);
        } else {
            verificationCheckRepository.save(MerchantVerificationCheck.builder()
                    .merchant(merchant)
                    .checkType(type)
                    .status(status)
                    .lastCheckedAt(Instant.now())
                    .build());
        }
    }

    private void transition(Merchant merchant, ApplicationWorkflowStatus status, String note) {
        merchant.setApplicationWorkflowStatus(status);
        historyRepository.save(MerchantApplicationStatusHistory.builder()
                .merchant(merchant)
                .workflowStatus(status)
                .occurredAt(Instant.now())
                .note(note)
                .build());
    }

    private void assertEditable(Merchant merchant) {
        var status = merchant.getApplicationWorkflowStatus();
        if (status == null || status == ApplicationWorkflowStatus.DRAFT || status == ApplicationWorkflowStatus.NEEDS_UPDATE) {
            return;
        }
        if (status == ApplicationWorkflowStatus.REJECTED && merchant.getReapplyAfter() != null
                && Instant.now().isAfter(merchant.getReapplyAfter())) {
            return;
        }
        throw MarketplaceException.badRequest("Application cannot be edited in status " + status);
    }

    private Merchant getOrCreateDraftMerchant() {
        var userId = requireUserId();
        return merchantRepository.findByOwnerId(userId).orElseGet(() -> {
            var owner = userRepository.findById(userId)
                    .orElseThrow(() -> MarketplaceException.notFound("User", userId));
            var merchant = Merchant.builder()
                    .owner(owner)
                    .businessName("Draft application")
                    .tinNumber("PENDING")
                    .status(MerchantStatus.DRAFT)
                    .kycStatus(KycStatus.NOT_STARTED)
                    .applicationWorkflowStatus(ApplicationWorkflowStatus.DRAFT)
                    .currentWizardStep(0)
                    .commissionRate(new BigDecimal("5.00"))
                    .build();
            merchant = merchantRepository.save(merchant);
            transition(merchant, ApplicationWorkflowStatus.DRAFT, "Application started");
            return merchant;
        });
    }

    private Merchant findOrNullMerchant() {
        var userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return null;
        }
        return merchantRepository.findByOwnerId(userId).orElse(null);
    }

    private Merchant getOwnedMerchant() {
        var userId = requireUserId();
        return merchantRepository.findByOwnerId(userId)
                .orElseThrow(() -> MarketplaceException.notFound("Merchant application", userId));
    }

    private UUID requireUserId() {
        var userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw MarketplaceException.unauthorized("Authentication required");
        }
        return userId;
    }

    private OnboardingDtos.WizardProgressResponse toProgress(Merchant merchant) {
        return OnboardingDtos.WizardProgressResponse.builder()
                .merchantId(merchant.getId())
                .applicationNumber(merchant.getApplicationNumber())
                .workflowStatus(merchant.getApplicationWorkflowStatus())
                .currentWizardStep(merchant.getCurrentWizardStep())
                .draftSavedAt(merchant.getDraftSavedAt())
                .readyToSubmit(merchant.getCurrentWizardStep() >= 6 && isDraftComplete(merchant))
                .build();
    }

    private boolean isDraftComplete(Merchant merchant) {
        try {
            validateReadyToSubmit(merchant);
            return true;
        } catch (MarketplaceException ex) {
            return false;
        }
    }

    private OnboardingDtos.ApplicationStatusResponse buildStatusResponse(Merchant merchant) {
        var timeline = historyRepository.findByMerchantIdOrderByOccurredAtAsc(merchant.getId()).stream()
                .map(h -> OnboardingDtos.StatusTimelineItem.builder()
                        .status(h.getWorkflowStatus())
                        .occurredAt(h.getOccurredAt())
                        .build())
                .toList();

        var checks = verificationCheckRepository.findByMerchantId(merchant.getId()).stream()
                .map(c -> OnboardingDtos.VerificationCheckItem.builder()
                        .checkType(c.getCheckType())
                        .status(c.getStatus())
                        .lastCheckedAt(c.getLastCheckedAt())
                        .build())
                .toList();

        var notes = reviewNoteRepository.findByMerchantIdOrderByCreatedAtAsc(merchant.getId()).stream()
                .filter(n -> !n.isResolved())
                .map(n -> OnboardingDtos.ReviewNoteItem.builder()
                        .id(n.getId())
                        .fieldKey(n.getFieldKey())
                        .message(n.getMessage())
                        .resolved(n.isResolved())
                        .build())
                .toList();

        List<String> suspendedFeatures = null;
        if (merchant.getApplicationWorkflowStatus() == ApplicationWorkflowStatus.SUSPENDED) {
            suspendedFeatures = List.of("Finance and payouts", "Order history and invoices", "Support tickets");
        }

        return OnboardingDtos.ApplicationStatusResponse.builder()
                .merchantId(merchant.getId())
                .applicationNumber(merchant.getApplicationNumber())
                .businessName(merchant.getBusinessName())
                .mainCategory(merchant.getMainCategory())
                .workflowStatus(merchant.getApplicationWorkflowStatus())
                .timeline(timeline)
                .checks(checks)
                .reviewerNotes(notes)
                .fixByDeadline(merchant.getFixByDeadline())
                .reapplyAfter(merchant.getReapplyAfter())
                .decisionReference(merchant.getDecisionReference())
                .suspensionReason(merchant.getSuspensionReason())
                .availableWhenSuspended(suspendedFeatures)
                .build();
    }
}
