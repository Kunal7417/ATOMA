package com.atoma.marketplace.onboarding.service;

import com.atoma.marketplace.auth.repository.UserRepository;
import com.atoma.marketplace.auth.security.SecurityUtils;
import com.atoma.marketplace.common.enums.ApplicationWorkflowStatus;
import com.atoma.marketplace.common.enums.BusinessType;
import com.atoma.marketplace.common.enums.KycStatus;
import com.atoma.marketplace.common.enums.MerchantStatus;
import com.atoma.marketplace.common.exception.ErrorCodes;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.merchant.entity.Merchant;
import com.atoma.marketplace.merchant.repository.MerchantRepository;
import com.atoma.marketplace.onboarding.dto.OnboardingApiDtos;
import com.atoma.marketplace.onboarding.entity.IdempotencyRecord;
import com.atoma.marketplace.onboarding.repository.IdempotencyRecordRepository;
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
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public OnboardingApiDtos.ConfigResponse getConfig() {
        var categories = categoryRepository.findAll().stream()
                .filter(Category::isActive)
                .map(c -> OnboardingApiDtos.CategoryOption.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .build())
                .toList();
        var businessTypes = java.util.Arrays.stream(BusinessType.values())
                .map(bt -> OnboardingApiDtos.BusinessTypeOption.builder()
                        .value(bt.name())
                        .label(formatLabel(bt.name()))
                        .build())
                .toList();
        return OnboardingApiDtos.ConfigResponse.builder()
                .businessTypes(businessTypes)
                .categories(categories)
                .steps(List.of(
                        step(1, "BUSINESS", "Business details"),
                        step(2, "OWNER", "Owner / representative"),
                        step(3, "DOCUMENTS", "Documents"),
                        step(4, "STORE_ADDRESS", "Store address"),
                        step(5, "PAYOUT", "Payout"),
                        step(6, "TERMS", "Terms & consent")
                ))
                .fieldRules(Map.of(
                        "businessName", rule(2, 200, true),
                        "primaryCategoryId", rule(0, 0, true)
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
    public OnboardingApiDtos.ApplicationResponse getApplication() {
        var merchant = findOwnedMerchantOrNull();
        if (merchant == null) {
            return emptyApplication();
        }
        return toApplicationResponse(merchant);
    }

    @Transactional
    public OnboardingApiDtos.ApplicationResponse patchBusiness(OnboardingApiDtos.PatchBusinessRequest request) {
        var merchant = getOrCreateDraftMerchant();
        assertBusinessEditable(merchant);
        assertVersion(merchant, request.version());
        applyBusinessPatch(merchant, request);
        merchant.setApplicationVersion(merchant.getApplicationVersion() + 1);
        merchant.setDraftSavedAt(Instant.now());
        merchant.setCurrentWizardStep(Math.max(merchant.getCurrentWizardStep(), 1));
        merchantRepository.save(merchant);
        return toApplicationResponse(merchant);
    }

    @Transactional
    public OnboardingApiDtos.SubmitBusinessResponse submitBusiness(
            OnboardingApiDtos.SubmitBusinessRequest request,
            String idempotencyKey
    ) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var cached = idempotencyRecordRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (cached.isPresent() && cached.get().getExpiresAt().isAfter(Instant.now())) {
                try {
                    return objectMapper.readValue(cached.get().getResponseBody(), OnboardingApiDtos.SubmitBusinessResponse.class);
                } catch (JsonProcessingException ignored) {
                    // fall through
                }
            }
        }

        var merchant = getOrCreateDraftMerchant();
        assertBusinessEditable(merchant);
        assertVersion(merchant, request.version());
        merchant.setBusinessType(request.businessType());
        merchant.setBusinessName(request.businessName());
        merchant.setPrimaryCategoryId(request.primaryCategoryId());
        categoryRepository.findById(request.primaryCategoryId()).ifPresent(c -> merchant.setMainCategory(c.getName()));
        merchant.setApplicationVersion(merchant.getApplicationVersion() + 1);
        merchant.setCurrentWizardStep(Math.max(merchant.getCurrentWizardStep(), 1));
        merchant.setDraftSavedAt(Instant.now());
        merchantRepository.save(merchant);

        var response = OnboardingApiDtos.SubmitBusinessResponse.builder()
                .application(toApplicationResponse(merchant))
                .nextStep("OWNER")
                .build();
        storeIdempotency(idempotencyKey, response);
        return response;
    }

    private void storeIdempotency(String idempotencyKey, OnboardingApiDtos.SubmitBusinessResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        try {
            idempotencyRecordRepository.save(IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey.trim())
                    .responseBody(objectMapper.writeValueAsString(response))
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void applyBusinessPatch(Merchant merchant, OnboardingApiDtos.PatchBusinessRequest request) {
        if (request.businessType() != null) {
            merchant.setBusinessType(request.businessType());
        }
        if (request.businessName() != null) {
            merchant.setBusinessName(request.businessName());
        }
        if (request.primaryCategoryId() != null) {
            merchant.setPrimaryCategoryId(request.primaryCategoryId());
            categoryRepository.findById(request.primaryCategoryId()).ifPresent(c -> merchant.setMainCategory(c.getName()));
        }
    }

    private void assertVersion(Merchant merchant, long expectedVersion) {
        if (merchant.getApplicationVersion() != expectedVersion) {
            throw MarketplaceException.of(
                    HttpStatus.CONFLICT,
                    ErrorCodes.VERSION_CONFLICT,
                    "Application version conflict",
                    null,
                    null
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

    private OnboardingApiDtos.ApplicationResponse toApplicationResponse(Merchant merchant) {
        var business = OnboardingApiDtos.BusinessStepData.builder()
                .businessType(merchant.getBusinessType())
                .businessName(merchant.getBusinessName())
                .primaryCategoryId(merchant.getPrimaryCategoryId())
                .primaryCategoryName(merchant.getMainCategory())
                .build();
        var completed = merchant.getCurrentWizardStep() >= 1 && merchant.getBusinessType() != null
                ? List.of("BUSINESS")
                : List.<String>of();
        return OnboardingApiDtos.ApplicationResponse.builder()
                .applicationId(merchant.getId())
                .version(merchant.getApplicationVersion())
                .currentStep(merchant.getCurrentWizardStep() == 0 ? 1 : merchant.getCurrentWizardStep())
                .completedSteps(completed)
                .business(business)
                .lastSavedAt(merchant.getDraftSavedAt())
                .workflowStatus(merchant.getApplicationWorkflowStatus() != null
                        ? merchant.getApplicationWorkflowStatus().name()
                        : ApplicationWorkflowStatus.DRAFT.name())
                .build();
    }

    private static OnboardingApiDtos.StepDefinition step(int n, String key, String title) {
        return OnboardingApiDtos.StepDefinition.builder().step(n).key(key).title(title).build();
    }

    private static OnboardingApiDtos.FieldRule rule(int min, int max, boolean required) {
        return OnboardingApiDtos.FieldRule.builder()
                .minLength(min)
                .maxLength(max)
                .required(required)
                .build();
    }

    private static String formatLabel(String enumName) {
        return enumName.replace('_', ' ');
    }
}
