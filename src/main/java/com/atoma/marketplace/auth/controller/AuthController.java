package com.atoma.marketplace.auth.controller;

import com.atoma.marketplace.auth.dto.AuthDtos;
import com.atoma.marketplace.auth.dto.MerchantAuthDtos;
import com.atoma.marketplace.auth.service.AuthService;
import com.atoma.marketplace.auth.service.MerchantOtpService;
import com.atoma.marketplace.auth.service.OtpAuthService;
import com.atoma.marketplace.auth.service.RefreshTokenService;
import com.atoma.marketplace.common.exception.MarketplaceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Unified JWT authentication for all surfaces")
public class AuthController {

    private final AuthService authService;
    private final OtpAuthService otpAuthService;
    private final MerchantOtpService merchantOtpService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new marketplace user")
    public AuthDtos.AuthResponse register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and receive JWT access token")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/otp/request")
    @Operation(summary = "Request OTP (merchant mobile sign-in)")
    public MerchantAuthDtos.OtpChallengeResponse requestOtp(@Valid @RequestBody MerchantAuthDtos.OtpRequest request) {
        return merchantOtpService.requestCode(request);
    }

    @PostMapping("/otp/send")
    @Operation(summary = "Legacy: send OTP by channel + identifier")
    public AuthDtos.OtpSendResponse sendOtp(@Valid @RequestBody AuthDtos.OtpSendRequest request) {
        return otpAuthService.sendCode(request);
    }

    @PostMapping("/otp/resend")
    @Operation(summary = "Resend OTP for an existing requestId")
    public MerchantAuthDtos.OtpChallengeResponse resendOtp(@Valid @RequestBody MerchantAuthDtos.OtpResendRequest request) {
        return merchantOtpService.resendCode(request.requestId());
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify OTP (requestId for mobile API, or legacy channel + identifier)")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody AuthDtos.OtpVerifyBody request) {
        if (request.requestId() != null) {
            var challenge = new MerchantAuthDtos.OtpVerifyChallengeRequest(
                    request.requestId(),
                    request.code(),
                    request.device()
            );
            return ResponseEntity.ok(merchantOtpService.verify(challenge));
        }
        if (request.channel() != null && request.identifier() != null) {
            var legacy = new AuthDtos.OtpVerifyRequest(request.channel(), request.identifier(), request.code());
            return ResponseEntity.ok(otpAuthService.verifyCode(legacy));
        }
        throw MarketplaceException.badRequest("Provide requestId or channel and identifier");
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public MerchantAuthDtos.RefreshTokenResponse refresh(@Valid @RequestBody MerchantAuthDtos.RefreshRequest request) {
        var bundle = refreshTokenService.refresh(request.refreshToken());
        var tokens = MerchantAuthDtos.TokenPair.builder()
                .accessToken(bundle.accessToken())
                .refreshToken(bundle.refreshToken())
                .tokenType("Bearer")
                .expiresIn(bundle.accessExpiresInSeconds())
                .build();
        return MerchantAuthDtos.RefreshTokenResponse.builder().tokens(tokens).build();
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke refresh token")
    public void logout(@Valid @RequestBody MerchantAuthDtos.LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }
}
