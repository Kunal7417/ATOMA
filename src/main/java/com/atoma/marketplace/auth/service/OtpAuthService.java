package com.atoma.marketplace.auth.service;

import com.atoma.marketplace.auth.dto.AuthDtos;
import com.atoma.marketplace.auth.entity.User;
import com.atoma.marketplace.auth.otp.OtpStore;
import com.atoma.marketplace.auth.repository.UserRepository;
import com.atoma.marketplace.auth.security.JwtTokenProvider;
import com.atoma.marketplace.auth.security.MarketplaceUserDetails;
import com.atoma.marketplace.common.enums.OtpChannel;
import com.atoma.marketplace.common.enums.UserRole;
import com.atoma.marketplace.common.enums.UserStatus;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.config.OtpProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtpAuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpStore otpStore;
    private final OtpProperties otpProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthDtos.OtpSendResponse sendCode(AuthDtos.OtpSendRequest request) {
        var key = otpKey(request.channel(), request.identifier());
        var code = generateCode();
        otpStore.save(key, code, Duration.ofMinutes(otpProperties.getTtlMinutes()));

        var builder = AuthDtos.OtpSendResponse.builder()
                .message("Verification code sent")
                .expiresInSeconds(otpProperties.getTtlMinutes() * 60);
        if (otpProperties.isExposeCode()) {
            builder.devCode(code);
        }
        return builder.build();
    }

    @Transactional
    public AuthDtos.AuthResponse verifyCode(AuthDtos.OtpVerifyRequest request) {
        var key = otpKey(request.channel(), request.identifier());
        var stored = otpStore.get(key);
        var valid = stored.isPresent() && stored.get().equals(request.code());
        if (!valid && otpProperties.isExposeCode() && otpProperties.getDevBypassCode().equals(request.code())) {
            valid = true;
        }
        if (!valid) {
            throw MarketplaceException.unauthorized("Invalid or expired verification code");
        }
        otpStore.delete(key);

        var user = findOrCreateMerchantUser(request);
        if (request.channel() == OtpChannel.PHONE) {
            user.setPhone(normalizePhone(request.identifier()));
        }
        userRepository.save(user);

        return buildAuthResponse(new MarketplaceUserDetails(user));
    }

    private User findOrCreateMerchantUser(AuthDtos.OtpVerifyRequest request) {
        if (request.channel() == OtpChannel.EMAIL) {
            return userRepository.findByEmail(request.identifier().trim().toLowerCase())
                    .orElseGet(() -> createOtpUser(request.identifier().trim().toLowerCase(), null));
        }
        var phone = normalizePhone(request.identifier());
        return userRepository.findByPhone(phone)
                .orElseGet(() -> createOtpUser(placeholderEmail(phone), phone));
    }

    private User createOtpUser(String email, String phone) {
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

    private String otpKey(OtpChannel channel, String identifier) {
        return channel.name() + ":" + identifier.trim().toLowerCase();
    }

    private String generateCode() {
        var bound = (int) Math.pow(10, otpProperties.getLength());
        var code = RANDOM.nextInt(bound);
        return String.format("%0" + otpProperties.getLength() + "d", code);
    }

    private AuthDtos.AuthResponse buildAuthResponse(MarketplaceUserDetails userDetails) {
        return AuthDtos.AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateToken(userDetails))
                .tokenType("Bearer")
                .userId(userDetails.getId())
                .email(userDetails.getEmail())
                .roles(userDetails.getAuthorities().stream()
                        .map(a -> UserRole.valueOf(a.getAuthority().replace("ROLE_", "")))
                        .collect(java.util.stream.Collectors.toSet()))
                .build();
    }
}
