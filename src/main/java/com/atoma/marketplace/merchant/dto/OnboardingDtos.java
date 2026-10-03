package com.atoma.marketplace.merchant.dto;

import com.atoma.marketplace.common.enums.ApplicationWorkflowStatus;
import com.atoma.marketplace.common.enums.BusinessType;
import com.atoma.marketplace.common.enums.PayoutMethod;
import com.atoma.marketplace.common.enums.RepresentativeRole;
import com.atoma.marketplace.common.enums.VerificationCheckStatus;
import com.atoma.marketplace.common.enums.VerificationCheckType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class OnboardingDtos {

    public record BusinessStepRequest(
            @NotNull BusinessType businessType,
            @NotBlank String businessName,
            @NotBlank String mainCategory
    ) {}

    public record OwnerStepRequest(
            @NotNull RepresentativeRole representativeRole,
            @NotBlank String fullName,
            String email
    ) {}

    public record DocumentItemRequest(
            @NotBlank String documentType,
            @NotBlank String fileUrl,
            String documentNumber,
            LocalDate expiryDate
    ) {}

    public record DocumentsStepRequest(
            @NotEmpty List<@Valid DocumentItemRequest> documents
    ) {}

    public record StoreAddressStepRequest(
            @NotBlank String buildingNumber,
            @NotBlank String street,
            @NotBlank String district,
            @NotBlank String city,
            String landmark,
            @NotNull BigDecimal latitude,
            @NotNull BigDecimal longitude
    ) {}

    public record PayoutStepRequest(
            @NotNull PayoutMethod payoutMethod,
            String bankAccountHint
    ) {}

    public record ConsentItemRequest(
            @NotBlank String consentKey,
            @NotBlank String policyVersion,
            boolean accepted
    ) {}

    public record TermsStepRequest(
            @NotEmpty List<@Valid ConsentItemRequest> consents
    ) {}

    @Value
    @Builder
    public static class WizardProgressResponse {
        UUID merchantId;
        String applicationNumber;
        ApplicationWorkflowStatus workflowStatus;
        int currentWizardStep;
        Instant draftSavedAt;
        boolean readyToSubmit;
    }

    @Value
    @Builder
    public static class ApplicationStatusResponse {
        UUID merchantId;
        String applicationNumber;
        String businessName;
        String mainCategory;
        ApplicationWorkflowStatus workflowStatus;
        List<StatusTimelineItem> timeline;
        List<VerificationCheckItem> checks;
        List<ReviewNoteItem> reviewerNotes;
        Instant fixByDeadline;
        Instant reapplyAfter;
        String decisionReference;
        String suspensionReason;
        List<String> availableWhenSuspended;
    }

    @Value
    @Builder
    public static class StatusTimelineItem {
        ApplicationWorkflowStatus status;
        Instant occurredAt;
    }

    @Value
    @Builder
    public static class VerificationCheckItem {
        VerificationCheckType checkType;
        VerificationCheckStatus status;
        Instant lastCheckedAt;
    }

    @Value
    @Builder
    public static class ReviewNoteItem {
        UUID id;
        String fieldKey;
        String message;
        boolean resolved;
    }

    @Value
    @Builder
    public static class SubmitApplicationResponse {
        String applicationNumber;
        ApplicationWorkflowStatus workflowStatus;
        String message;
    }

    public record ReviewNoteInput(
            @NotBlank String fieldKey,
            @NotBlank String message
    ) {}

    public record AdminApplicationReviewRequest(
            @NotNull AdminReviewAction action,
            List<@Valid ReviewNoteInput> reviewerNotes,
            String rejectionReason,
            Instant reapplyAfter,
            String decisionReference,
            Instant fixByDeadline
    ) {}

    public enum AdminReviewAction {
        START_REVIEW,
        REQUEST_UPDATES,
        APPROVE,
        REJECT
    }

    public record SuspendMerchantRequest(
            @NotBlank String reason
    ) {}

    public record LicenceRenewalRequest(
            @NotBlank String fileUrl,
            @NotBlank String licenceNumber,
            LocalDate expiryDate
    ) {}

    public record WizardStepRequest(
            @Min(1) @Max(7) int step,
            BusinessStepRequest business,
            OwnerStepRequest owner,
            DocumentsStepRequest documents,
            StoreAddressStepRequest storeAddress,
            PayoutStepRequest payout,
            TermsStepRequest terms
    ) {}
}
