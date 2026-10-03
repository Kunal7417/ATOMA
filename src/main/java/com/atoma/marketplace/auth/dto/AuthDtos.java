package com.atoma.marketplace.auth.dto;

import com.atoma.marketplace.common.enums.OtpChannel;
import com.atoma.marketplace.common.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.util.Set;
import java.util.UUID;

public class AuthDtos {

    @Value
    @Builder
    public static class AuthResponse {
        String accessToken;
        String tokenType;
        UUID userId;
        String email;
        Set<UserRole> roles;
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 20) String phone,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotEmpty Set<UserRole> roles
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record OtpSendRequest(
            @NotNull OtpChannel channel,
            @NotBlank String identifier
    ) {}

    public record OtpVerifyRequest(
            @NotNull OtpChannel channel,
            @NotBlank String identifier,
            @NotBlank String code
    ) {}

    /** Supports mobile verify (requestId) and legacy verify (channel + identifier). */
    public record OtpVerifyBody(
            UUID requestId,
            @NotBlank String code,
            MerchantAuthDtos.DeviceInfo device,
            OtpChannel channel,
            String identifier
    ) {}

    @Value
    @Builder
    public static class OtpSendResponse {
        String message;
        int expiresInSeconds;
        String devCode;
    }
}
