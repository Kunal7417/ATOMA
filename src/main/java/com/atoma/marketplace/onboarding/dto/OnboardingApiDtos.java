package com.atoma.marketplace.onboarding.dto;

import com.atoma.marketplace.common.enums.BusinessType;
import com.atoma.marketplace.common.enums.PayoutMethod;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OnboardingApiDtos {

    @Value
    @Builder
    public static class ConfigResponse {
        List<BusinessTypeOption> businessTypes;
        List<CategoryOption> categories;
        List<StepDefinition> steps;
        Map<String, FieldRule> fieldRules;
    }

    @Value
    @Builder
    public static class BusinessTypeOption {
        String value;
        String label;
    }

    @Value
    @Builder
    public static class CategoryOption {
        UUID id;
        String name;
    }

    @Value
    @Builder
    public static class StepDefinition {
        int step;
        String key;
        String title;
    }

    @Value
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FieldRule {
        Integer minLength;
        Integer maxLength;
        boolean required;
    }

    @Value
    @Builder
    public static class BusinessStepData {
        BusinessType businessType;
        String businessName;
        UUID primaryCategoryId;
        String primaryCategoryName;
    }

    @Value
    @Builder
    public static class StatusHistoryItem {
        String status;
        Instant at;
    }

    @Value
    @Builder
    public static class CheckItem {
        String id;
        String status;
        Integer done;
        Integer total;
    }

    @Value
    @Builder
    public static class ReviewerNoteItem {
        UUID id;
        String step;
        String field;
        String title;
        String message;
    }

    @Value
    @Builder
    public static class DecisionInfo {
        String reason;
        String reference;
        LocalDate reapplyFrom;
    }

    @Value
    @Builder
    public static class SuspensionInfo {
        String reason;
        String remedy;
        List<String> stillAvailable;
    }

    @Value
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApplicationResponse {
        UUID applicationId;
        String reference;
        long version;
        int currentStep;
        List<String> completedSteps;
        BusinessStepData business;
        Instant lastSavedAt;
        String workflowStatus;
        List<StatusHistoryItem> statusHistory;
        List<CheckItem> checks;
        Instant checksUpdatedAt;
        List<ReviewerNoteItem> reviewerNotes;
        LocalDate fixBy;
        DecisionInfo decision;
        SuspensionInfo suspension;
    }

    public record PatchBusinessRequest(
            @NotNull Long version,
            BusinessType businessType,
            String businessName,
            @JsonDeserialize(using = LenientUuidDeserializer.class) UUID primaryCategoryId
    ) {}

    /** Same fields as patch; {@code primaryCategoryId} JSON {@code ""} clears the category. */
    @Getter
    @Setter
    public static class PatchBusinessRequestBody {
        @NotNull
        private Long version;
        private BusinessType businessType;
        private String businessName;
        @JsonIgnore
        private boolean primaryCategorySpecified;
        @Getter
        @Setter(AccessLevel.NONE)
        private UUID primaryCategoryId;

        @JsonProperty("primaryCategoryId")
        public void bindPrimaryCategory(String raw) {
            primaryCategorySpecified = true;
            if (raw == null || raw.isBlank()) {
                primaryCategoryId = null;
            } else {
                primaryCategoryId = UUID.fromString(raw.trim());
            }
        }

        public OnboardingApiDtos.PatchBusinessRequest toPatchRequest() {
            return new PatchBusinessRequest(version, businessType, businessName, primaryCategoryId);
        }
    }

    public record SubmitBusinessRequest(
            @NotNull Long version,
            @NotNull BusinessType businessType,
            @NotBlank String businessName,
            @NotNull UUID primaryCategoryId
    ) {}

    public record PatchOwnerRequest(
            @NotNull Long version,
            String fullName,
            String fatherName,
            String tazkiraNumber,
            LocalDate dateOfBirth,
            String email
    ) {}

    public record SubmitOwnerRequest(
            @NotNull Long version,
            @NotBlank String fullName,
            @NotBlank String fatherName,
            @NotBlank String tazkiraNumber,
            @NotNull LocalDate dateOfBirth,
            String email
    ) {}

    public record PatchDocumentsRequest(
            @NotNull Long version,
            String licenceNumber,
            LocalDate licenceExpiry,
            String tin,
            java.util.List<java.util.UUID> documentIds
    ) {}

    public record SubmitDocumentsRequest(
            @NotNull Long version,
            @NotBlank String licenceNumber,
            @NotNull LocalDate licenceExpiry,
            String tin,
            java.util.List<java.util.UUID> documentIds
    ) {}

    public record PatchStoreAddressRequest(
            @NotNull Long version,
            String province,
            String district,
            String street,
            String landmark
    ) {}

    public record SubmitStoreAddressRequest(
            @NotNull Long version,
            @NotBlank String province,
            @NotBlank String district,
            @NotBlank String street,
            String landmark
    ) {}

    public record PatchPayoutRequest(
            @NotNull Long version,
            PayoutMethod method,
            String bankId,
            String walletProviderId,
            String accountName,
            String accountNumber,
            String walletNumber
    ) {}

    public record SubmitPayoutRequest(
            @NotNull Long version,
            @NotNull PayoutMethod method,
            String bankId,
            String walletProviderId,
            @NotBlank String accountName,
            String accountNumber,
            String walletNumber
    ) {}

    public record PatchTermsRequest(
            @NotNull Long version,
            Boolean merchantTerms,
            Boolean accurateInfo,
            Boolean verificationConsent,
            String termsVersion
    ) {}

    public record SubmitTermsRequest(
            @NotNull Long version,
            @NotNull Boolean merchantTerms,
            @NotNull Boolean accurateInfo,
            @NotNull Boolean verificationConsent,
            @NotBlank String termsVersion
    ) {}

    @Value
    @Builder
    public static class SubmitStepResponse {
        ApplicationResponse application;
        String nextStep;
    }

    @Value
    @Builder
    public static class SubmitBusinessResponse {
        ApplicationResponse application;
        String nextStep;
    }

    @Value
    @Builder
    public static class UploadedDocumentResponse {
        UUID id;
        String kind;
        String fileName;
        Instant uploadedAt;
        String previewUrl;
    }

    @Value
    @Builder
    public static class PayoutInstitutionItem {
        String id;
        String name;
        String type;
    }

    public record LicenceRenewalRequest(
            @NotNull java.util.UUID documentId,
            @NotBlank String licenceNumber,
            @NotNull LocalDate licenceExpiry
    ) {}

    @Value
    @Builder
    public static class SubmitApplicationResponse {
        ApplicationResponse application;
    }

    @Value
    @Builder
    public static class PayoutInstitutionsResponse {
        List<PayoutInstitutionItem> banks;
        List<PayoutInstitutionItem> walletProviders;
    }
}
