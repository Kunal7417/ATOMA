package com.atoma.marketplace.onboarding.service;

import com.atoma.marketplace.auth.repository.UserRepository;
import com.atoma.marketplace.auth.security.SecurityUtils;
import com.atoma.marketplace.common.enums.ApplicationWorkflowStatus;
import com.atoma.marketplace.common.enums.BusinessType;
import com.atoma.marketplace.common.enums.KycStatus;
import com.atoma.marketplace.common.enums.MerchantStatus;
import com.atoma.marketplace.common.exception.ErrorCodes;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.common.i18n.MerchantAppLanguage;
import com.atoma.marketplace.merchant.entity.Merchant;
import com.atoma.marketplace.merchant.repository.MerchantRepository;
import com.atoma.marketplace.merchant.repository.MerchantApplicationStatusHistoryRepository;
import com.atoma.marketplace.merchant.repository.MerchantReviewNoteRepository;
import com.atoma.marketplace.merchant.repository.MerchantVerificationCheckRepository;
import com.atoma.marketplace.merchant.service.ApplicationNumberGenerator;
import com.atoma.marketplace.onboarding.dto.OnboardingApiDtos;
import com.atoma.marketplace.merchant.entity.MerchantVerificationCheck;
import com.atoma.marketplace.common.enums.VerificationCheckStatus;
import com.atoma.marketplace.onboarding.i18n.OnboardingLocalization;
import com.atoma.marketplace.product.entity.Category;
import com.atoma.marketplace.product.repository.CategoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantOnboardingApiService {

    private static final Set<ApplicationWorkflowStatus> LOCKED_STATUSES = Set.of(
            ApplicationWorkflowStatus.SUBMITTED,
            ApplicationWorkflowStatus.UNDER_REVIEW,
            ApplicationWorkflowStatus.APPROVED,
            ApplicationWorkflowStatus.SUSPENDED
    );

    private final CategoryRepository categoryRepository;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;
    private final OnboardingLocalization onboardingLocalization;
    private final OnboardingIdempotencySupport idempotencySupport;
    private final MerchantApplicationStatusHistoryRepository historyRepository;
    private final MerchantReviewNoteRepository reviewNoteRepository;
    private final MerchantVerificationCheckRepository verificationCheckRepository;
    private final ApplicationNumberGenerator applicationNumberGenerator;

    @Transactional(readOnly = true)
    public OnboardingApiDtos.ConfigResponse getConfig(MerchantAppLanguage language) {
        var categories = categoryRepository.findAll().stream()
                .filter(Category::isActive)
                .map(c -> OnboardingApiDtos.CategoryOption.builder()
                        .id(c.getId())
                        .name(onboardingLocalization.categoryName(c.getName(), language))
                        .build())
                .toList();
        var businessTypes = java.util.Arrays.stream(BusinessType.values())
                .map(bt -> OnboardingApiDtos.BusinessTypeOption.builder()
                        .value(bt.name())
                        .label(onboardingLocalization.businessTypeLabel(bt, language))
                        .build())
                .toList();
        return OnboardingApiDtos.ConfigResponse.builder()
                .businessTypes(businessTypes)
                .categories(categories)
                .steps(List.of(
                        step(1, "BUSINESS", language, "Business details"),
                        step(2, "OWNER", language, "Owner / representative"),
                        step(3, "DOCUMENTS", language, "Documents"),
                        step(4, "STORE_ADDRESS", language, "Store address"),
                        step(5, "PAYOUT", language, "Payout"),
                        step(6, "TERMS", language, "Terms & consent")
                ))
                .fieldRules(Map.of(
                        "businessName", OnboardingApiDtos.FieldRule.builder()
                                .required(true)
                                .minLength(2)
                                .maxLength(200)
                                .build(),
                        "primaryCategoryId", OnboardingApiDtos.FieldRule.builder()
                                .required(true)
                                .build()
                ))
                .build();
    }

    public String configEtag(OnboardingApiDtos.ConfigResponse config) {
        try {
            return "\"" + Integer.toHexString(objectMapper.writeValueAsString(config).hashCode()) + "\"";
        } catch (JsonProcessingException e) {
            return "\"0\"";
        }
    }

    @Transactional(readOnly = true)
    public OnboardingApiDtos.ApplicationResponse getApplication(MerchantAppLanguage language) {
        var merchant = findOwnedMerchantOrNull();
        if (merchant == null) {
            return emptyApplication();
        }
        return toApplicationResponse(merchant, language);
    }

    @Transactional
    public OnboardingApiDtos.ApplicationResponse patchBusiness(
            OnboardingApiDtos.PatchBusinessRequestBody request,
            MerchantAppLanguage language
    ) {
        var merchant = getOrCreateDraftMerchant();
        assertBusinessEditable(merchant);
        assertVersion(merchant, request.getVersion());
        applyBusinessPatch(merchant, request);
        merchant.setApplicationVersion(merchant.getApplicationVersion() + 1);
        merchant.setDraftSavedAt(Instant.now());
        merchant.setCurrentWizardStep(Math.max(merchant.getCurrentWizardStep(), 1));
        merchantRepository.save(merchant);
        return toApplicationResponse(merchant, language);
    }

    @Transactional
    public OnboardingApiDtos.SubmitBusinessResponse submitBusiness(
            OnboardingApiDtos.SubmitBusinessRequest request,
            String idempotencyKey
    ) {
        var replay = idempotencySupport.replayIfMatches(
                idempotencyKey, request, OnboardingApiDtos.SubmitBusinessResponse.class);
        if (replay.isPresent()) {
            return replay.get();
        }

        var merchant = getOrCreateDraftMerchant();
        assertBusinessEditable(merchant);
        assertVersion(merchant, request.version());
        merchant.setBusinessType(request.businessType());
        merchant.setBusinessName(request.businessName());
        merchant.setPrimaryCategoryId(request.primaryCategoryId());
        categoryRepository.findById(request.primaryCategoryId()).ifPresent(c -> merchant.setMainCategory(c.getName()));
        merchant.setApplicationVersion(merchant.getApplicationVersion() + 1);
        merchant.setCurrentWizardStep(2);
        merchant.setDraftSavedAt(Instant.now());
        if (merchant.getApplicationNumber() == null) {
            merchant.setApplicationNumber(applicationNumberGenerator.generate());
        }
        merchantRepository.save(merchant);

        var response = OnboardingApiDtos.SubmitBusinessResponse.builder()
                .application(toApplicationResponse(merchant, MerchantAppLanguage.EN))
                .nextStep("OWNER")
                .build();
        idempotencySupport.store(idempotencyKey, request, response);
        return response;
    }

    private void applyBusinessPatch(Merchant merchant, OnboardingApiDtos.PatchBusinessRequestBody request) {
        if (request.getBusinessType() != null) {
            merchant.setBusinessType(request.getBusinessType());
        }
        if (request.getBusinessName() != null) {
            merchant.setBusinessName(request.getBusinessName());
        }
        if (request.isPrimaryCategorySpecified()) {
            merchant.setPrimaryCategoryId(request.getPrimaryCategoryId());
            if (request.getPrimaryCategoryId() == null) {
                merchant.setMainCategory(null);
            } else {
                categoryRepository.findById(request.getPrimaryCategoryId())
                        .ifPresent(c -> merchant.setMainCategory(c.getName()));
            }
        }
    }

    private void assertVersion(Merchant merchant, long expectedVersion) {
        if (merchant.getApplicationVersion() != expectedVersion) {
            throw MarketplaceException.withDetails(
                    HttpStatus.CONFLICT,
                    ErrorCodes.VERSION_CONFLICT,
                    "Application version conflict",
                    Map.of("currentVersion", merchant.getApplicationVersion())
            );
        }
    }

    private void assertBusinessEditable(Merchant merchant) {
        var status = merchant.getApplicationWorkflowStatus();
        if (status != null && LOCKED_STATUSES.contains(status)) {
            throw MarketplaceException.of(
                    HttpStatus.CONFLICT,
                    ErrorCodes.APPLICATION_LOCKED,
                    "Application is locked for editing",
                    null,
                    null
            );
        }
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
                    .applicationVersion(0L)
                    .commissionRate(new BigDecimal("5.00"))
                    .build();
            return merchantRepository.save(merchant);
        });
    }

    private Merchant findOwnedMerchantOrNull() {
        var userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            return null;
        }
        return merchantRepository.findByOwnerId(userId).orElse(null);
    }

    private UUID requireUserId() {
        var userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw MarketplaceException.unauthorized("Authentication required");
        }
        return userId;
    }

    private OnboardingApiDtos.ApplicationResponse emptyApplication() {
        return OnboardingApiDtos.ApplicationResponse.builder()
                .version(0L)
                .currentStep(1)
                .completedSteps(List.of())
                .business(null)
                .lastSavedAt(null)
                .workflowStatus(ApplicationWorkflowStatus.DRAFT.name())
                .build();
    }

    private OnboardingApiDtos.ApplicationResponse toApplicationResponse(Merchant merchant, MerchantAppLanguage language) {
        var categoryName = merchant.getMainCategory();
        if (categoryName != null && language != null) {
            categoryName = onboardingLocalization.categoryName(categoryName, language);
        }
        var business = OnboardingApiDtos.BusinessStepData.builder()
                .businessType(merchant.getBusinessType())
                .businessName(merchant.getBusinessName())
                .primaryCategoryId(merchant.getPrimaryCategoryId())
                .primaryCategoryName(categoryName)
                .build();
        return OnboardingApiDtos.ApplicationResponse.builder()
                .applicationId(merchant.getId())
                .reference(merchant.getApplicationNumber())
                .version(merchant.getApplicationVersion())
                .currentStep(merchant.getCurrentWizardStep() == 0 ? 1 : merchant.getCurrentWizardStep())
                .completedSteps(completedSteps(merchant))
                .business(business)
                .lastSavedAt(merchant.getDraftSavedAt())
                .workflowStatus(merchant.getApplicationWorkflowStatus() != null
                        ? merchant.getApplicationWorkflowStatus().name()
                        : ApplicationWorkflowStatus.DRAFT.name())
                .statusHistory(loadStatusHistory(merchant))
                .checks(loadChecks(merchant))
                .checksUpdatedAt(merchant.getChecksUpdatedAt())
                .reviewerNotes(loadReviewerNotes(merchant, language))
                .fixBy(merchant.getFixByDeadline() != null
                        ? LocalDate.ofInstant(merchant.getFixByDeadline(), java.time.ZoneOffset.UTC)
                        : null)
                .decision(buildDecision(merchant, language))
                .suspension(buildSuspension(merchant, language))
                .build();
    }

    private List<String> completedSteps(Merchant merchant) {
        var steps = new ArrayList<String>();
        if (merchant.getCurrentWizardStep() >= 2 && merchant.getPrimaryCategoryId() != null) {
            steps.add("BUSINESS");
        }
        if (merchant.getCurrentWizardStep() >= 3 && merchant.getOwnerFullName() != null) {
            steps.add("OWNER");
        }
        if (merchant.getCurrentWizardStep() >= 4 && merchant.getBusinessLicenseNumber() != null) {
            steps.add("DOCUMENTS");
        }
        if (merchant.getCurrentWizardStep() >= 5 && merchant.getStreet() != null) {
            steps.add("STORE_ADDRESS");
        }
        if (merchant.getCurrentWizardStep() >= 6 && merchant.getPayoutMethod() != null) {
            steps.add("PAYOUT");
        }
        if (merchant.getCurrentWizardStep() >= 7) {
            steps.add("TERMS");
        }
        return steps;
    }

    private List<OnboardingApiDtos.StatusHistoryItem> loadStatusHistory(Merchant merchant) {
        if (merchant.getId() == null) {
            return List.of();
        }
        return historyRepository.findByMerchantIdOrderByOccurredAtAsc(merchant.getId()).stream()
                .map(h -> OnboardingApiDtos.StatusHistoryItem.builder()
                        .status(h.getWorkflowStatus().name())
                        .at(h.getOccurredAt())
                        .build())
                .toList();
    }

    private List<OnboardingApiDtos.CheckItem> loadChecks(Merchant merchant) {
        if (merchant.getId() == null) {
            return List.of();
        }
        return verificationCheckRepository.findByMerchantId(merchant.getId()).stream()
                .sorted(Comparator.comparing(MerchantVerificationCheck::getCheckType))
                .map(c -> {
                    int total = 1;
                    int done = c.getStatus() == VerificationCheckStatus.DONE ? 1 : 0;
                    return OnboardingApiDtos.CheckItem.builder()
                            .id(c.getCheckType().name())
                            .status(c.getStatus().name())
                            .done(done)
                            .total(total)
                            .build();
                })
                .toList();
    }

    private List<OnboardingApiDtos.ReviewerNoteItem> loadReviewerNotes(Merchant merchant, MerchantAppLanguage language) {
        if (merchant.getId() == null) {
            return List.of();
        }
        return reviewNoteRepository.findByMerchantIdAndResolvedFalseOrderByCreatedAtAsc(merchant.getId()).stream()
                .map(n -> {
                    var step = n.getStepKey() != null ? n.getStepKey() : n.getFieldKey();
                    var field = n.getFieldKey();
                    var titleKey = n.getNoteTitleKey() != null ? n.getNoteTitleKey() : n.getFieldKey();
                    return OnboardingApiDtos.ReviewerNoteItem.builder()
                            .id(n.getId())
                            .step(step)
                            .field(field)
                            .title(onboardingLocalization.reviewerNoteTitle(titleKey, language))
                            .message(onboardingLocalization.displayText(n.getMessage(), language))
                            .build();
                })
                .toList();
    }

    private OnboardingApiDtos.DecisionInfo buildDecision(Merchant merchant, MerchantAppLanguage language) {
        if (merchant.getApplicationWorkflowStatus() != ApplicationWorkflowStatus.REJECTED) {
            return null;
        }
        return OnboardingApiDtos.DecisionInfo.builder()
                .reason(onboardingLocalization.rejectionReason(merchant.getRejectionReason(), language))
                .reference(merchant.getDecisionReference())
                .reapplyFrom(merchant.getReapplyAfter() != null
                        ? LocalDate.ofInstant(merchant.getReapplyAfter(), java.time.ZoneOffset.UTC)
                        : null)
                .build();
    }

    private OnboardingApiDtos.SuspensionInfo buildSuspension(Merchant merchant, MerchantAppLanguage language) {
        if (merchant.getApplicationWorkflowStatus() != ApplicationWorkflowStatus.SUSPENDED) {
            return null;
        }
        return OnboardingApiDtos.SuspensionInfo.builder()
                .reason(onboardingLocalization.suspensionReason(merchant.getSuspensionReason(), language))
                .remedy("RENEW_LICENCE")
                .stillAvailable(List.of("PAYOUTS", "ORDERS", "SUPPORT"))
                .build();
    }

    public Merchant findOwnedMerchantOrNullPublic() {
        return findOwnedMerchantOrNull();
    }

    public Merchant requireEditableDraftMerchant() {
        var merchant = getOrCreateDraftMerchant();
        assertBusinessEditable(merchant);
        return merchant;
    }

    public void assertVersionPublic(Merchant merchant, long expectedVersion) {
        assertVersion(merchant, expectedVersion);
    }

    public OnboardingApiDtos.ApplicationResponse toApplicationResponsePublic(Merchant merchant, MerchantAppLanguage language) {
        return toApplicationResponse(merchant, language);
    }

    private OnboardingApiDtos.StepDefinition step(int n, String key, MerchantAppLanguage language, String englishTitle) {
        return OnboardingApiDtos.StepDefinition.builder()
                .step(n)
                .key(key)
                .title(onboardingLocalization.stepTitle(key, language, englishTitle))
                .build();
    }
}
