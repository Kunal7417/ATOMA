package com.atoma.marketplace.onboarding.controller;

import com.atoma.marketplace.onboarding.dto.OnboardingApiDtos;
import com.atoma.marketplace.onboarding.service.MerchantOnboardingApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Tag(name = "Merchant Onboarding (Mobile API)", description = "Config and step-1 business application")
public class OnboardingController {

    private final MerchantOnboardingApiService onboardingApiService;

    @GetMapping("/config")
    @Operation(summary = "Onboarding metadata (business types, categories, steps)")
    public ResponseEntity<OnboardingApiDtos.ConfigResponse> config(
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        var config = onboardingApiService.getConfig();
        var etag = onboardingApiService.configEtag(config);
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(etag).build();
        }
        return ResponseEntity.ok().eTag(etag).body(config);
    }

    @GetMapping("/application")
    @Operation(summary = "Load merchant onboarding application draft")
    public OnboardingApiDtos.ApplicationResponse application() {
        return onboardingApiService.getApplication();
    }

    @PatchMapping("/application/business")
    @Operation(summary = "Save business step draft (autosave)")
    public OnboardingApiDtos.ApplicationResponse patchBusiness(
            @Valid @RequestBody OnboardingApiDtos.PatchBusinessRequest request
    ) {
        return onboardingApiService.patchBusiness(request);
    }

    @PostMapping("/application/business/submit")
    @Operation(summary = "Submit business step and continue")
    public OnboardingApiDtos.SubmitBusinessResponse submitBusiness(
            @Valid @RequestBody OnboardingApiDtos.SubmitBusinessRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return onboardingApiService.submitBusiness(request, idempotencyKey);
    }
}
