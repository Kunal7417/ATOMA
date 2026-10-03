package com.atoma.marketplace.merchant.controller;

import com.atoma.marketplace.merchant.dto.OnboardingDtos;
import com.atoma.marketplace.merchant.service.MerchantOnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/onboarding")
@RequiredArgsConstructor
@Tag(name = "Merchant Onboarding V2", description = "7-step verification wizard and application status")
public class MerchantOnboardingController {

    private final MerchantOnboardingService onboardingService;

    @GetMapping("/progress")
    @Operation(summary = "Wizard progress for current merchant")
    public OnboardingDtos.WizardProgressResponse progress() {
        return onboardingService.getProgress();
    }

    @PutMapping("/wizard/step")
    @Operation(summary = "Save a wizard step (1-6)")
    public OnboardingDtos.WizardProgressResponse saveStep(@Valid @RequestBody OnboardingDtos.WizardStepRequest request) {
        return onboardingService.saveWizardStep(request);
    }

    @PostMapping("/wizard/draft")
    @Operation(summary = "Explicit save draft timestamp")
    public OnboardingDtos.WizardProgressResponse saveDraft() {
        return onboardingService.saveDraft();
    }

    @PostMapping("/application/submit")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Submit application for review")
    public OnboardingDtos.SubmitApplicationResponse submit() {
        return onboardingService.submitApplication();
    }

    @PostMapping("/application/resubmit")
    @Operation(summary = "Resubmit after reviewer requested updates")
    public OnboardingDtos.SubmitApplicationResponse resubmit() {
        return onboardingService.resubmitAfterUpdates();
    }

    @GetMapping("/application/status")
    @Operation(summary = "Application timeline, checks, and reviewer notes")
    public OnboardingDtos.ApplicationStatusResponse status() {
        return onboardingService.getApplicationStatus();
    }

    @PostMapping("/licence/renewal")
    @Operation(summary = "Upload renewed licence when suspended")
    public OnboardingDtos.ApplicationStatusResponse licenceRenewal(
            @Valid @RequestBody OnboardingDtos.LicenceRenewalRequest request
    ) {
        return onboardingService.submitLicenceRenewal(request);
    }
}
