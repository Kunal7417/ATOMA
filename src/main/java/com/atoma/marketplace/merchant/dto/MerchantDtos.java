package com.atoma.marketplace.merchant.dto;

import com.atoma.marketplace.common.enums.KycStatus;
import com.atoma.marketplace.common.enums.MerchantStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

public class MerchantDtos {

    public record MerchantOnboardRequest(
            @NotBlank String businessName,
            String description,
            @NotBlank String tinNumber,
            String businessLicenseNumber,
            BigDecimal latitude,
            BigDecimal longitude,
            @NotBlank String address,
            @NotBlank String city
    ) {}

    public record KycDocumentRequest(
            @NotBlank String documentType,
            @NotBlank String fileUrl
    ) {}

    @Value
    @Builder
    public static class KycDocumentResponse {
        UUID id;
        String documentType;
        String fileUrl;
        KycStatus reviewStatus;
        String reviewNotes;
    }

    @Value
    @Builder
    public static class MerchantResponse {
        UUID id;
        UUID ownerUserId;
        String businessName;
        String description;
        MerchantStatus status;
        KycStatus kycStatus;
        String address;
        String city;
        BigDecimal commissionRate;
        boolean provisionalActive;
    }
}
