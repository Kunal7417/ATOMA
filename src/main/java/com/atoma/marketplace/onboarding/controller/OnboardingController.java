package com.atoma.marketplace.onboarding.controller;

import com.atoma.marketplace.common.i18n.MerchantAppLanguage;
import com.atoma.marketplace.onboarding.dto.OnboardingApiDtos;
import com.atoma.marketplace.onboarding.service.MerchantOnboardingApiService;
import com.atoma.marketplace.onboarding.service.OnboardingApplicationStepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Tag(name = "Merchant Onboarding (Mobile API)", description = "Config and step application")
public class OnboardingController {

    private final MerchantOnboardingApiService onboardingApiService;
    private final OnboardingApplicationStepService stepService;

    @GetMapping("/config")
    @Operation(summary = "Onboarding metadata (business types, categories, steps)")
    public ResponseEntity<OnboardingApiDtos.ConfigResponse> config(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        var language = MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage);
        var config = onboardingApiService.getConfig(language);
        var etag = onboardingApiService.configEtag(config);
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).body(config);
    }

    @GetMapping("/application")
    @Operation(summary = "Load merchant onboarding application draft")
    public OnboardingApiDtos.ApplicationResponse application(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage
    ) {
        return onboardingApiService.getApplication(MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage));
    }

    @PatchMapping("/application/business")
    @Operation(summary = "Save business step draft (autosave)")
    public OnboardingApiDtos.ApplicationResponse patchBusiness(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage,
            @Valid @RequestBody OnboardingApiDtos.PatchBusinessRequestBody request
    ) {
        return onboardingApiService.patchBusiness(
                request, MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage));
    }

    @PostMapping("/application/business/submit")
    @Operation(summary = "Submit business step and continue")
    public OnboardingApiDtos.SubmitBusinessResponse submitBusiness(
            @Valid @RequestBody OnboardingApiDtos.SubmitBusinessRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return onboardingApiService.submitBusiness(request, idempotencyKey);
    }

    @PatchMapping("/application/owner")
    public OnboardingApiDtos.ApplicationResponse patchOwner(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage,
            @Valid @RequestBody OnboardingApiDtos.PatchOwnerRequest request
    ) {
        return stepService.patchOwner(request, MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage));
    }

    @PostMapping("/application/owner/submit")
    public OnboardingApiDtos.SubmitStepResponse submitOwner(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OnboardingApiDtos.SubmitOwnerRequest request
    ) {
        return stepService.submitOwner(request, idempotencyKey, MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage));
    }

    @PatchMapping("/application/documents")
    public OnboardingApiDtos.ApplicationResponse patchDocuments(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage,
            @Valid @RequestBody OnboardingApiDtos.PatchDocumentsRequest request
    ) {
        return stepService.patchDocuments(request, MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage));
    }

    @PostMapping("/application/documents/submit")
    public OnboardingApiDtos.SubmitStepResponse submitDocuments(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OnboardingApiDtos.SubmitDocumentsRequest request
    ) {
        return stepService.submitDocuments(request, idempotencyKey, MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage));
    }

    @PatchMapping("/application/store-address")
    public OnboardingApiDtos.ApplicationResponse patchStoreAddress(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage,
            @Valid @RequestBody OnboardingApiDtos.PatchStoreAddressRequest request
    ) {
        return stepService.patchStoreAddress(request, MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage));
    }

    @PostMapping("/application/store-address/submit")
    public OnboardingApiDtos.SubmitStepResponse submitStoreAddress(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OnboardingApiDtos.SubmitStoreAddressRequest request
    ) {
        return stepService.submitStoreAddress(request, idempotencyKey, MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage));
    }

    @PatchMapping("/application/payout")
    public OnboardingApiDtos.ApplicationResponse patchPayout(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage,
            @Valid @RequestBody OnboardingApiDtos.PatchPayoutRequest request
    ) {
        return stepService.patchPayout(request, MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage));
    }

    @PostMapping("/application/payout/submit")
    public OnboardingApiDtos.SubmitStepResponse submitPayout(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OnboardingApiDtos.SubmitPayoutRequest request
    ) {
        return stepService.submitPayout(request, idempotencyKey, MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage));
    }

    @PatchMapping("/application/terms")
    public OnboardingApiDtos.ApplicationResponse patchTerms(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage,
            @Valid @RequestBody OnboardingApiDtos.PatchTermsRequest request
    ) {
        return stepService.patchTerms(request, MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage));
    }

    @PostMapping("/application/terms/submit")
    public OnboardingApiDtos.SubmitStepResponse submitTerms(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody OnboardingApiDtos.SubmitTermsRequest request
    ) {
        return stepService.submitTerms(request, idempotencyKey, MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage));
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OnboardingApiDtos.UploadedDocumentResponse uploadDocument(
            @RequestParam("kind") String kind,
            @RequestPart("file") MultipartFile file
    ) {
        return stepService.uploadDocument(kind, file);
    }

    @PostMapping("/application/submit")
    public OnboardingApiDtos.SubmitApplicationResponse submitApplication(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return stepService.submitApplication(idempotencyKey, MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage));
    }

    @DeleteMapping("/application")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discardApplication() {
        stepService.discardDraft();
    }

    @PostMapping("/application/licence-renewal")
    public OnboardingApiDtos.ApplicationResponse licenceRenewal(
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, defaultValue = "en") String acceptLanguage,
            @Valid @RequestBody OnboardingApiDtos.LicenceRenewalRequest request
    ) {
        return stepService.licenceRenewal(request, MerchantAppLanguage.fromAcceptLanguageHeader(acceptLanguage));
    }

    @GetMapping("/reference/payout-institutions")
    public OnboardingApiDtos.PayoutInstitutionsResponse payoutInstitutions() {
        return stepService.payoutInstitutions();
    }
}
