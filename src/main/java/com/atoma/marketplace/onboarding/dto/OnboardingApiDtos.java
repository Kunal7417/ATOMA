package com.atoma.marketplace.onboarding.dto;

import com.atoma.marketplace.common.enums.BusinessType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
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
    public static class FieldRule {
        int minLength;
        int maxLength;
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
    public static class ApplicationResponse {
        UUID applicationId;
        long version;
        int currentStep;
        List<String> completedSteps;
        BusinessStepData business;
        Instant lastSavedAt;
        String workflowStatus;
    }

    public record PatchBusinessRequest(
            @NotNull Long version,
            BusinessType businessType,
            String businessName,
            UUID primaryCategoryId
    ) {}

    public record SubmitBusinessRequest(
            @NotNull Long version,
            @NotNull BusinessType businessType,
            @NotBlank String businessName,
            @NotNull UUID primaryCategoryId
    ) {}

    @Value
    @Builder
    public static class SubmitBusinessResponse {
        ApplicationResponse application;
        String nextStep;
    }
}
