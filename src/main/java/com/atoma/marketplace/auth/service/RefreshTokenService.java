package com.atoma.marketplace.auth.service;

import com.atoma.marketplace.auth.entity.RefreshToken;
import com.atoma.marketplace.auth.entity.User;
import com.atoma.marketplace.auth.repository.RefreshTokenRepository;
import com.atoma.marketplace.auth.security.JwtTokenProvider;
import com.atoma.marketplace.auth.security.MarketplaceUserDetails;
import com.atoma.marketplace.common.exception.ErrorCodes;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final long REFRESH_TTL_SECONDS = 30L * 24 * 60 * 60;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityProperties securityProperties;

    @Transactional
    public MerchantTokenBundle issueTokens(User user) {
        var details = new MarketplaceUserDetails(user);
        var access = jwtTokenProvider.generateToken(details);
        var rawRefresh = UUID.randomUUID().toString() + "." + UUID.randomUUID();
        var hash = hash(rawRefresh);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .tokenHash(hash)
                .expiresAt(Instant.now().plusSeconds(REFRESH_TTL_SECONDS))
                .build());
        return new MerchantTokenBundle(
                access,
                rawRefresh,
                securityProperties.getJwtExpirationMs() / 1000
        );
    }

    @Transactional
    public MerchantTokenBundle refresh(String rawRefreshToken) {
        var hash = hash(rawRefreshToken);
        var stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> MarketplaceException.of(
                        HttpStatus.UNAUTHORIZED,
                        ErrorCodes.REFRESH_TOKEN_INVALID,
                        "Refresh token invalid",
                        null,
                        null
                ));
        if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(Instant.now())) {
            throw MarketplaceException.of(
                    HttpStatus.UNAUTHORIZED,
                    ErrorCodes.REFRESH_TOKEN_INVALID,
                    "Refresh token expired or revoked",
                    null,
                    null
            );
        }
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);
        return issueTokens(stored.getUser());
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        var hash = hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    public static String hash(String raw) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record MerchantTokenBundle(String accessToken, String refreshToken, long accessExpiresInSeconds) {}
}
