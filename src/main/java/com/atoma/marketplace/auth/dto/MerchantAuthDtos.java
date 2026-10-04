package com.atoma.marketplace.auth.dto;

import com.atoma.marketplace.common.enums.OtpDeliveryChannel;
import com.atoma.marketplace.auth.validation.AfghanMobilePhone;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.util.Set;
import java.util.UUID;

public class MerchantAuthDtos {

    public record OtpRequest(
            @NotNull OtpDeliveryChannel channel,
            @AfghanMobilePhone String phone,
            @Email String email
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
    public static class RefreshTokenResponse {
        TokenPair tokens;
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
        @JsonProperty("name")
        String name;
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
        @JsonProperty("isNewUser")
        boolean isNewUser;

        @JsonProperty("newUser")
        public boolean getNewUser() {
            return isNewUser;
        }
    }

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record LogoutRequest(@NotBlank String refreshToken) {}
}
