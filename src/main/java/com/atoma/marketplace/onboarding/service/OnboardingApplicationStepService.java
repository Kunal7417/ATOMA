package com.atoma.marketplace.onboarding.service;

import com.atoma.marketplace.common.enums.ApplicationWorkflowStatus;
import com.atoma.marketplace.common.enums.PayoutMethod;
import com.atoma.marketplace.common.exception.ErrorCodes;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.common.i18n.MerchantAppLanguage;
import com.atoma.marketplace.merchant.entity.Merchant;
import com.atoma.marketplace.merchant.entity.MerchantConsent;
import com.atoma.marketplace.merchant.repository.MerchantConsentRepository;
import com.atoma.marketplace.merchant.repository.MerchantRepository;
import com.atoma.marketplace.onboarding.dto.OnboardingApiDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OnboardingApplicationStepService {

    private final MerchantOnboardingApiService onboardingApiService;
    private final MerchantRepository merchantRepository;
    private final MerchantConsentRepository consentRepository;
    private final OnboardingIdempotencySupport idempotencySupport;
    private final OnboardingDocumentUploadService documentUploadService;

    @Transactional
    public OnboardingApiDtos.ApplicationResponse patchOwner(OnboardingApiDtos.PatchOwnerRequest request, MerchantAppLanguage language) {
        var merchant = requireDraftMerchant();
        onboardingApiService.assertVersionPublic(merchant, request.version());
        if (request.fullName() != null) {
            merchant.setOwnerFullName(request.fullName());
        }
        if (request.fatherName() != null) {
            merchant.setOwnerFatherName(request.fatherName());
        }
        if (request.tazkiraNumber() != null) {
            merchant.setOwnerTazkiraNumber(request.tazkiraNumber());
        }
        if (request.dateOfBirth() != null) {
            merchant.setOwnerDateOfBirth(request.dateOfBirth());
        }
        if (request.email() != null) {
            merchant.setOwnerEmail(request.email());
        }
        bump(merchant);
        return onboardingApiService.toApplicationResponsePublic(merchant, language);
    }

    @Transactional
    public OnboardingApiDtos.SubmitStepResponse submitOwner(
            OnboardingApiDtos.SubmitOwnerRequest request,
            String idempotencyKey,
            MerchantAppLanguage language
    ) {
        return submitStep(idempotencyKey, request, () -> {
            var merchant = requireDraftMerchant();
            onboardingApiService.assertVersionPublic(merchant, request.version());
            merchant.setOwnerFullName(request.fullName());
            merchant.setOwnerFatherName(request.fatherName());
            merchant.setOwnerTazkiraNumber(request.tazkiraNumber());
            merchant.setOwnerDateOfBirth(request.dateOfBirth());
            merchant.setOwnerEmail(request.email());
            merchant.setCurrentWizardStep(3);
            bump(merchant);
            return buildSubmitResponse(merchant, language, "DOCUMENTS");
        });
    }

    @Transactional
    public OnboardingApiDtos.ApplicationResponse patchDocuments(OnboardingApiDtos.PatchDocumentsRequest request, MerchantAppLanguage language) {
        var merchant = requireDraftMerchant();
        onboardingApiService.assertVersionPublic(merchant, request.version());
        if (request.licenceNumber() != null) {
            merchant.setBusinessLicenseNumber(request.licenceNumber());
        }
        if (request.licenceExpiry() != null) {
            merchant.setLicenceExpiry(request.licenceExpiry());
        }
        if (request.tin() != null) {
            merchant.setTinNumber(request.tin());
        }
        documentUploadService.assertOwnedDocumentIds(merchant, request.documentIds());
        bump(merchant);
        return onboardingApiService.toApplicationResponsePublic(merchant, language);
    }

    @Transactional
    public OnboardingApiDtos.SubmitStepResponse submitDocuments(
            OnboardingApiDtos.SubmitDocumentsRequest request,
            String idempotencyKey,
            MerchantAppLanguage language
    ) {
        return submitStep(idempotencyKey, request, () -> {
            var merchant = requireDraftMerchant();
            onboardingApiService.assertVersionPublic(merchant, request.version());
            merchant.setBusinessLicenseNumber(request.licenceNumber());
            merchant.setLicenceExpiry(request.licenceExpiry());
            if (request.tin() != null) {
                merchant.setTinNumber(request.tin());
            }
            documentUploadService.assertOwnedDocumentIds(merchant, request.documentIds());
            merchant.setCurrentWizardStep(4);
            bump(merchant);
            return buildSubmitResponse(merchant, language, "STORE_ADDRESS");
        });
    }

    @Transactional
    public OnboardingApiDtos.ApplicationResponse patchStoreAddress(
            OnboardingApiDtos.PatchStoreAddressRequest request,
            MerchantAppLanguage language
    ) {
        var merchant = requireDraftMerchant();
        onboardingApiService.assertVersionPublic(merchant, request.version());
        if (request.province() != null) {
            merchant.setProvince(request.province());
        }
        if (request.district() != null) {
            merchant.setDistrict(request.district());
        }
        if (request.street() != null) {
            merchant.setStreet(request.street());
        }
        if (request.landmark() != null) {
            merchant.setLandmark(request.landmark());
        }
        bump(merchant);
        return onboardingApiService.toApplicationResponsePublic(merchant, language);
    }

    @Transactional
    public OnboardingApiDtos.SubmitStepResponse submitStoreAddress(
            OnboardingApiDtos.SubmitStoreAddressRequest request,
            String idempotencyKey,
            MerchantAppLanguage language
    ) {
        return submitStep(idempotencyKey, request, () -> {
            var merchant = requireDraftMerchant();
            onboardingApiService.assertVersionPublic(merchant, request.version());
            merchant.setProvince(request.province());
            merchant.setDistrict(request.district());
            merchant.setStreet(request.street());
            merchant.setLandmark(request.landmark());
            merchant.setCurrentWizardStep(5);
            bump(merchant);
            return buildSubmitResponse(merchant, language, "PAYOUT");
        });
    }

    @Transactional
    public OnboardingApiDtos.ApplicationResponse patchPayout(OnboardingApiDtos.PatchPayoutRequest request, MerchantAppLanguage language) {
        var merchant = requireDraftMerchant();
        onboardingApiService.assertVersionPublic(merchant, request.version());
        if (request.method() != null) {
            merchant.setPayoutMethod(request.method());
        }
        if (request.bankId() != null) {
            merchant.setPayoutBankId(request.bankId());
        }
        if (request.walletProviderId() != null) {
            merchant.setPayoutWalletProviderId(request.walletProviderId());
        }
        if (request.accountName() != null) {
            merchant.setPayoutAccountName(request.accountName());
        }
        if (request.accountNumber() != null || request.walletNumber() != null) {
            var raw = request.method() == PayoutMethod.WALLET ? request.walletNumber() : request.accountNumber();
            merchant.setBankAccountHint(maskAccount(raw));
        }
        bump(merchant);
        return onboardingApiService.toApplicationResponsePublic(merchant, language);
    }

    @Transactional
    public OnboardingApiDtos.SubmitStepResponse submitPayout(
            OnboardingApiDtos.SubmitPayoutRequest request,
            String idempotencyKey,
            MerchantAppLanguage language
    ) {
        return submitStep(idempotencyKey, request, () -> {
            var merchant = requireDraftMerchant();
            onboardingApiService.assertVersionPublic(merchant, request.version());
            validatePayoutSubmit(request.method(), request.bankId(), request.walletProviderId(),
                    request.accountNumber(), request.walletNumber());
            merchant.setPayoutMethod(request.method());
            merchant.setPayoutBankId(request.bankId());
            merchant.setPayoutWalletProviderId(request.walletProviderId());
            merchant.setPayoutAccountName(request.accountName());
            merchant.setBankAccountHint(maskAccount(
                    request.method() == PayoutMethod.WALLET ? request.walletNumber() : request.accountNumber()));
            merchant.setCurrentWizardStep(6);
            bump(merchant);
            return buildSubmitResponse(merchant, language, "TERMS");
        });
    }

    @Transactional
    public OnboardingApiDtos.ApplicationResponse patchTerms(OnboardingApiDtos.PatchTermsRequest request, MerchantAppLanguage language) {
        var merchant = requireDraftMerchant();
        onboardingApiService.assertVersionPublic(merchant, request.version());
        bump(merchant);
        return onboardingApiService.toApplicationResponsePublic(merchant, language);
    }

    @Transactional
    public OnboardingApiDtos.SubmitStepResponse submitTerms(
            OnboardingApiDtos.SubmitTermsRequest request,
            String idempotencyKey,
            MerchantAppLanguage language
    ) {
        return submitStep(idempotencyKey, request, () -> {
            var merchant = requireDraftMerchant();
            onboardingApiService.assertVersionPublic(merchant, request.version());
            if (!Boolean.TRUE.equals(request.merchantTerms())
                    || !Boolean.TRUE.equals(request.accurateInfo())
                    || !Boolean.TRUE.equals(request.verificationConsent())) {
                throw MarketplaceException.withDetails(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        ErrorCodes.VALIDATION_FAILED,
                        "All consents required",
                        java.util.Map.of("fields", java.util.Map.of("terms", "REQUIRED"))
                );
            }
            consentRepository.save(MerchantConsent.builder()
                    .merchant(merchant)
                    .consentKey("MERCHANT_TERMS")
                    .policyVersion(request.termsVersion())
                    .acceptedAt(Instant.now())
                    .build());
            merchant.setCurrentWizardStep(7);
            bump(merchant);
            return buildSubmitResponse(merchant, language, null);
        });
    }

    @Transactional
    public OnboardingApiDtos.UploadedDocumentResponse uploadDocument(String kind, MultipartFile file) {
        var merchant = requireDraftMerchant();
        return documentUploadService.store(merchant, kind, file);
    }

    @Transactional
    public OnboardingApiDtos.ApplicationResponse licenceRenewal(
            OnboardingApiDtos.LicenceRenewalRequest request,
            MerchantAppLanguage language
    ) {
        var merchant = onboardingApiService.findOwnedMerchantOrNullPublic();
        if (merchant == null) {
            throw MarketplaceException.notFound("Application", "current");
        }
        if (merchant.getApplicationWorkflowStatus() != ApplicationWorkflowStatus.SUSPENDED) {
            throw MarketplaceException.of(
                    HttpStatus.CONFLICT,
                    ErrorCodes.APPLICATION_LOCKED,
                    "Licence renewal only applies to suspended merchants",
                    null,
                    null
            );
        }
        documentUploadService.assertOwnedDocumentIds(merchant, List.of(request.documentId()));
        merchant.setBusinessLicenseNumber(request.licenceNumber());
        merchant.setLicenceExpiry(request.licenceExpiry());
        merchant.setApplicationWorkflowStatus(ApplicationWorkflowStatus.UNDER_REVIEW);
        merchant.setChecksUpdatedAt(Instant.now());
        merchant.setApplicationVersion(merchant.getApplicationVersion() + 1);
        merchantRepository.save(merchant);
        return onboardingApiService.toApplicationResponsePublic(merchant, language);
    }

    @Transactional
    public OnboardingApiDtos.SubmitApplicationResponse submitApplication(
            String idempotencyKey,
            MerchantAppLanguage language
    ) {
        var replay = idempotencySupport.replayIfMatches(
                idempotencyKey, Map.of("action", "FINAL_SUBMIT"), OnboardingApiDtos.SubmitApplicationResponse.class);
        if (replay.isPresent()) {
            return replay.get();
        }
        var merchant = requireDraftMerchant();
        if (merchant.getCurrentWizardStep() < 7) {
            throw MarketplaceException.withDetails(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ErrorCodes.VALIDATION_FAILED,
                    "Complete all steps before submitting",
                    java.util.Map.of("fields", java.util.Map.of("application", "INCOMPLETE"))
            );
        }
        merchant.setApplicationWorkflowStatus(ApplicationWorkflowStatus.SUBMITTED);
        merchant.setSubmittedAt(Instant.now());
        merchant.setApplicationVersion(merchant.getApplicationVersion() + 1);
        merchantRepository.save(merchant);
        var application = onboardingApiService.toApplicationResponsePublic(merchant, language);
        var response = OnboardingApiDtos.SubmitApplicationResponse.builder().application(application).build();
        idempotencySupport.store(idempotencyKey, Map.of("action", "FINAL_SUBMIT"), response);
        return response;
    }

    @Transactional
    public void discardDraft() {
        var merchant = onboardingApiService.findOwnedMerchantOrNullPublic();
        if (merchant == null) {
            return;
        }
        if (merchant.getApplicationWorkflowStatus() != ApplicationWorkflowStatus.DRAFT) {
            throw MarketplaceException.of(
                    HttpStatus.CONFLICT,
                    ErrorCodes.APPLICATION_LOCKED,
                    "Only DRAFT applications can be discarded",
                    null,
                    null
            );
        }
        merchantRepository.delete(merchant);
    }

    @Transactional(readOnly = true)
    public OnboardingApiDtos.PayoutInstitutionsResponse payoutInstitutions() {
        return OnboardingApiDtos.PayoutInstitutionsResponse.builder()
                .banks(List.of(
                        institution("awcc-bank", "Azizi Bank", "BANK"),
                        institution("ghazanfar", "Ghazanfar Bank", "BANK")
                ))
                .walletProviders(List.of(
                        institution("atoma-wallet", "ATOMA Pay Wallet", "WALLET"),
                        institution("m-paisa", "M-Paisa", "WALLET")
                ))
                .build();
    }

    private static OnboardingApiDtos.PayoutInstitutionItem institution(String id, String name, String type) {
        return OnboardingApiDtos.PayoutInstitutionItem.builder().id(id).name(name).type(type).build();
    }

    private OnboardingApiDtos.SubmitStepResponse buildSubmitResponse(
            Merchant merchant,
            MerchantAppLanguage language,
            String nextStep
    ) {
        return OnboardingApiDtos.SubmitStepResponse.builder()
                .application(onboardingApiService.toApplicationResponsePublic(merchant, language))
                .nextStep(nextStep)
                .build();
    }

    private <T> OnboardingApiDtos.SubmitStepResponse submitStep(
            String idempotencyKey,
            Object requestBody,
            java.util.function.Supplier<OnboardingApiDtos.SubmitStepResponse> action
    ) {
        var replay = idempotencySupport.replayIfMatches(idempotencyKey, requestBody, OnboardingApiDtos.SubmitStepResponse.class);
        if (replay.isPresent()) {
            return replay.get();
        }
        var response = action.get();
        idempotencySupport.store(idempotencyKey, requestBody, response);
        return response;
    }

    private Merchant requireDraftMerchant() {
        return onboardingApiService.requireEditableDraftMerchant();
    }

    private void bump(Merchant merchant) {
        merchant.setApplicationVersion(merchant.getApplicationVersion() + 1);
        merchant.setDraftSavedAt(Instant.now());
        merchantRepository.save(merchant);
    }

    private static String maskAccount(String account) {
        if (account == null || account.length() < 4) {
            return "****";
        }
        return "****" + account.substring(account.length() - 4);
    }

    private static void validatePayoutSubmit(
            PayoutMethod method,
            String bankId,
            String walletProviderId,
            String accountNumber,
            String walletNumber
    ) {
        if (method == PayoutMethod.BANK) {
            if (bankId == null || bankId.isBlank() || accountNumber == null || accountNumber.isBlank()) {
                throw MarketplaceException.withDetails(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        ErrorCodes.VALIDATION_FAILED,
                        "Bank payout details required",
                        java.util.Map.of("fields", java.util.Map.of("payout", "REQUIRED"))
                );
            }
        } else if (method == PayoutMethod.WALLET) {
            if (walletProviderId == null || walletProviderId.isBlank()
                    || walletNumber == null || walletNumber.isBlank()) {
                throw MarketplaceException.withDetails(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        ErrorCodes.VALIDATION_FAILED,
                        "Wallet payout details required",
                        java.util.Map.of("fields", java.util.Map.of("payout", "REQUIRED"))
                );
            }
        }
    }
}
