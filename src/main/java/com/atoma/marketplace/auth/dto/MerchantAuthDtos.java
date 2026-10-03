package com.atoma.marketplace.auth.dto;

import com.atoma.marketplace.common.enums.OtpDeliveryChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.util.Set;
import java.util.UUID;

public class MerchantAuthDtos {

    public record OtpRequest(
            @NotNull OtpDeliveryChannel channel,
            String phone,
            String email
    ) {}

    public record OtpResendRequest(
            @NotNull UUID requestId
    ) {}

    public record DeviceInfo(
            String model,
            String osVersion,
            String pushToken
    ) {}

    public record OtpVerifyChallengeRequest(
            @NotNull UUID requestId,
            @NotBlank String code,
            DeviceInfo device
    ) {}

    @Value
    @Builder
    public static class OtpChallengeResponse {
        UUID requestId;
        String maskedDestination;
        int codeLength;
        int expiresIn;
        int resendAfter;
        String devCode;
    }

    @Value
    @Builder
    public static class TokenPair {
        String accessToken;
        String refreshToken;
        String tokenType;
        long expiresIn;
    }

    @Value
    @Builder
    public static class UserSummary {
        UUID id;
        String email;
        String phone;
        Set<String> roles;
    }

    @Value
    @Builder
    public static class MerchantSummary {
        UUID id;
        String businessName;
        String status;
        String workflowStatus;
    }

    @Value
    @Builder
    public static class OtpVerifyResponse {
        TokenPair tokens;
        UserSummary user;
        MerchantSummary merchant;
        String nextRoute;
        boolean isNewUser;
    }

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record LogoutRequest(@NotBlank String refreshToken) {}
}
