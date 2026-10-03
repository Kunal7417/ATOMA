package com.atoma.marketplace.auth.service;

import com.atoma.marketplace.auth.dto.MerchantAuthDtos;
import com.atoma.marketplace.auth.entity.User;
import com.atoma.marketplace.auth.otp.OtpChallenge;
import com.atoma.marketplace.auth.otp.OtpChallengeStore;
import com.atoma.marketplace.auth.repository.UserRepository;
import com.atoma.marketplace.common.enums.OtpDeliveryChannel;
import com.atoma.marketplace.common.enums.UserRole;
import com.atoma.marketplace.common.enums.UserStatus;
import com.atoma.marketplace.common.exception.ErrorCodes;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.config.OtpProperties;
import com.atoma.marketplace.merchant.entity.Merchant;
import com.atoma.marketplace.merchant.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MerchantOtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpChallengeStore challengeStore;
    private final OtpProperties otpProperties;
    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    public MerchantAuthDtos.OtpChallengeResponse requestCode(MerchantAuthDtos.OtpRequest request) {
        var destination = resolveDestination(request);
        enforceHourlyLimit(request.channel(), destination);
        return createChallenge(request.channel(), destination);
    }

    public MerchantAuthDtos.OtpChallengeResponse resendCode(UUID requestId) {
        var existing = challengeStore.findById(requestId)
                .orElseThrow(() -> MarketplaceException.of(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.OTP_REQUEST_NOT_FOUND,
                        "OTP request not found",
                        null,
                        null
                ));
        if (existing.isSuperseded()) {
            throw MarketplaceException.of(
                    HttpStatus.NOT_FOUND,
                    ErrorCodes.OTP_REQUEST_NOT_FOUND,
                    "OTP request not found",
                    null,
                    null
            );
        }
        var now = Instant.now();
        if (now.isBefore(existing.getResendAllowedAt())) {
            var wait = Duration.between(now, existing.getResendAllowedAt()).getSeconds();
            throw MarketplaceException.of(
                    HttpStatus.TOO_MANY_REQUESTS,
                    ErrorCodes.RESEND_TOO_SOON,
                    "Resend not available yet",
                    wait,
                    null
            );
        }
        challengeStore.markSuperseded(requestId);
        enforceHourlyLimit(existing.getChannel(), existing.getDestination());
        return createChallenge(existing.getChannel(), existing.getDestination());
    }

    @Transactional
    public MerchantAuthDtos.OtpVerifyResponse verify(MerchantAuthDtos.OtpVerifyChallengeRequest request) {
        var challenge = challengeStore.findById(request.requestId())
                .orElseThrow(() -> MarketplaceException.of(
                        HttpStatus.NOT_FOUND,
                        ErrorCodes.OTP_REQUEST_NOT_FOUND,
                        "OTP request not found",
                        null,
                        null
                ));
        if (challenge.isSuperseded()) {
            throw MarketplaceException.of(
                    HttpStatus.NOT_FOUND,
                    ErrorCodes.OTP_REQUEST_NOT_FOUND,
                    "OTP request not found",
                    null,
                    null
            );
        }
        if (challenge.isConsumed()) {
            throw MarketplaceException.of(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.OTP_ALREADY_USED,
                    "Code already used",
                    null,
                    null
            );
        }
        if (Instant.now().isAfter(challenge.getExpiresAt())) {
            throw MarketplaceException.of(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.OTP_EXPIRED,
                    "Code expired",
                    null,
                    null
            );
        }
        if (challenge.getWrongAttempts() >= otpProperties.getMaxWrongAttempts()) {
            throw MarketplaceException.of(
                    HttpStatus.TOO_MANY_REQUESTS,
                    ErrorCodes.OTP_TOO_MANY_ATTEMPTS,
                    "Too many wrong attempts",
                    null,
                    0
            );
        }

        var codeValid = challenge.getCode().equals(request.code())
                || (otpProperties.isExposeCode() && otpProperties.getDevBypassCode().equals(request.code()));
        if (!codeValid) {
            challengeStore.incrementWrongAttempts(request.requestId());
            var remaining = otpProperties.getMaxWrongAttempts() - challenge.getWrongAttempts() - 1;
            throw MarketplaceException.of(
                    HttpStatus.BAD_REQUEST,
                    ErrorCodes.OTP_INVALID,
                    "Invalid verification code",
                    null,
                    Math.max(remaining, 0)
            );
        }

        challengeStore.markConsumed(request.requestId());
        var isNew = !userExistsForChallenge(challenge);
        var user = findOrCreateUser(challenge);
        userRepository.save(user);

        var tokens = refreshTokenService.issueTokens(user);
        var merchant = merchantRepository.findByOwnerId(user.getId()).orElse(null);
        return MerchantAuthDtos.OtpVerifyResponse.builder()
                .tokens(MerchantAuthDtos.TokenPair.builder()
                        .accessToken(tokens.accessToken())
                        .refreshToken(tokens.refreshToken())
                        .tokenType("Bearer")
                        .expiresIn(tokens.accessExpiresInSeconds())
                        .build())
                .user(toUserSummary(user))
                .merchant(merchant != null ? toMerchantSummary(merchant) : null)
                .nextRoute(resolveNextRoute(merchant))
                .isNewUser(isNew)
                .build();
    }

    private MerchantAuthDtos.OtpChallengeResponse createChallenge(OtpDeliveryChannel channel, String destination) {
        var now = Instant.now();
        var code = generateCode();
        var requestId = UUID.randomUUID();
        var ttl = Duration.ofMinutes(otpProperties.getTtlMinutes());
        var cooldown = Duration.ofSeconds(otpProperties.getResendCooldownSeconds());
        var challenge = OtpChallenge.builder()
                .requestId(requestId)
                .channel(channel)
                .destination(destination)
                .code(code)
                .expiresAt(now.plus(ttl))
                .resendAllowedAt(now.plus(cooldown))
                .consumed(false)
                .superseded(false)
                .wrongAttempts(0)
                .lastSentAt(now)
                .build();
        challengeStore.save(challenge);

        var builder = MerchantAuthDtos.OtpChallengeResponse.builder()
                .requestId(requestId)
                .maskedDestination(mask(destination, channel))
                .codeLength(otpProperties.getLength())
                .expiresIn((int) ttl.getSeconds())
                .resendAfter(otpProperties.getResendCooldownSeconds());
        if (otpProperties.isExposeCode()) {
            builder.devCode(code);
        }
        return builder.build();
    }

    private void enforceHourlyLimit(OtpDeliveryChannel channel, String destination) {
        var since = Instant.now().minus(Duration.ofHours(1));
        var key = channel.name() + ":" + destination.toLowerCase();
        if (challengeStore.countSendsForDestinationSince(key, since) >= otpProperties.getMaxAttemptsPerHour()) {
            throw MarketplaceException.of(
                    HttpStatus.TOO_MANY_REQUESTS,
                    ErrorCodes.RATE_LIMITED,
                    "Too many verification requests",
                    3600L,
                    null
            );
        }
    }

    private String resolveDestination(MerchantAuthDtos.OtpRequest request) {
        if (request.channel() == OtpDeliveryChannel.SMS) {
            if (request.phone() == null || request.phone().isBlank()) {
                throw MarketplaceException.badRequest("phone is required for SMS channel");
            }
            return normalizePhone(request.phone());
        }
        if (request.email() == null || request.email().isBlank()) {
            throw MarketplaceException.badRequest("email is required for EMAIL channel");
        }
        return request.email().trim().toLowerCase();
    }

    private boolean userExistsForChallenge(OtpChallenge challenge) {
        if (challenge.getChannel() == OtpDeliveryChannel.EMAIL) {
            return userRepository.findByEmail(challenge.getDestination()).isPresent();
        }
        return userRepository.findByPhone(challenge.getDestination()).isPresent();
    }

    private User findOrCreateUser(OtpChallenge challenge) {
        if (challenge.getChannel() == OtpDeliveryChannel.EMAIL) {
            return userRepository.findByEmail(challenge.getDestination())
                    .orElseGet(() -> createUser(challenge.getDestination(), null));
        }
        return userRepository.findByPhone(challenge.getDestination())
                .orElseGet(() -> createUser(placeholderEmail(challenge.getDestination()), challenge.getDestination()));
    }

    private User createUser(String email, String phone) {
        if (phone == null) {
            phone = "+93000" + String.format("%07d", Math.abs(email.hashCode() % 10_000_000));
        }
        if (userRepository.existsByEmail(email)) {
            throw MarketplaceException.conflict("Email already registered");
        }
        if (userRepository.existsByPhone(phone)) {
            throw MarketplaceException.conflict("Phone already registered");
        }
        return User.builder()
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .firstName("Merchant")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(UserRole.MERCHANT))
                .build();
    }

    private String placeholderEmail(String phone) {
        var digits = phone.replaceAll("\\D", "");
        return "merchant+" + digits + "@otp.atoma.local";
    }

    private String normalizePhone(String raw) {
        var trimmed = raw.trim();
        if (trimmed.startsWith("+")) {
            return trimmed;
        }
        return "+93" + trimmed.replaceAll("\\s", "");
    }

    private String generateCode() {
        var bound = (int) Math.pow(10, otpProperties.getLength());
        var code = RANDOM.nextInt(bound);
        return String.format("%0" + otpProperties.getLength() + "d", code);
    }

    private static String mask(String destination, OtpDeliveryChannel channel) {
        if (channel == OtpDeliveryChannel.EMAIL) {
            var at = destination.indexOf('@');
            if (at <= 1) {
                return "***" + destination.substring(at);
            }
            return destination.charAt(0) + "***" + destination.substring(at);
        }
        if (destination.length() <= 4) {
            return "****";
        }
        return destination.substring(0, Math.min(3, destination.length())) + "***"
                + destination.substring(destination.length() - 2);
    }

    private MerchantAuthDtos.UserSummary toUserSummary(User user) {
        return MerchantAuthDtos.UserSummary.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .build();
    }

    private MerchantAuthDtos.MerchantSummary toMerchantSummary(Merchant merchant) {
        return MerchantAuthDtos.MerchantSummary.builder()
                .id(merchant.getId())
                .businessName(merchant.getBusinessName())
                .status(merchant.getStatus() != null ? merchant.getStatus().name() : null)
                .workflowStatus(merchant.getApplicationWorkflowStatus() != null
                        ? merchant.getApplicationWorkflowStatus().name()
                        : null)
                .build();
    }

    private String resolveNextRoute(Merchant merchant) {
        if (merchant == null) {
            return "ONBOARDING_BUSINESS";
        }
        if (merchant.getApplicationWorkflowStatus() == null
                || merchant.getCurrentWizardStep() < 1
                || merchant.getBusinessType() == null) {
            return "ONBOARDING_BUSINESS";
        }
        if (merchant.getSubmittedAt() != null) {
            return "APPLICATION_STATUS";
        }
        return "ONBOARDING_CONTINUE";
    }
}
