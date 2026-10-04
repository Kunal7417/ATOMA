package com.atoma.marketplace.onboarding.service;

import com.atoma.marketplace.common.exception.ErrorCodes;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.onboarding.dto.OnboardingApiDtos;
import com.atoma.marketplace.onboarding.entity.IdempotencyRecord;
import com.atoma.marketplace.onboarding.repository.IdempotencyRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OnboardingIdempotencySupport {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final ObjectMapper objectMapper;

    public <T> Optional<T> replayIfMatches(String idempotencyKey, Object requestBody, Class<T> responseType) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        var key = idempotencyKey.trim();
        var hash = hashBody(requestBody, objectMapper);
        var cached = idempotencyRecordRepository.findByIdempotencyKey(key);
        if (cached.isEmpty() || cached.get().getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        var record = cached.get();
        if (record.getRequestBodyHash() != null && !record.getRequestBodyHash().equals(hash)) {
            throw MarketplaceException.withDetails(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ErrorCodes.VALIDATION_FAILED,
                    "Idempotency-Key reused with different request body",
                    Map.of("fields", Map.of("idempotencyKey", "CONFLICT"))
            );
        }
        try {
            return Optional.of(objectMapper.readValue(record.getResponseBody(), responseType));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public void store(String idempotencyKey, Object requestBody, Object response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        try {
            idempotencyRecordRepository.save(IdempotencyRecord.builder()
                    .idempotencyKey(idempotencyKey.trim())
                    .requestBodyHash(hashBody(requestBody, objectMapper))
                    .responseBody(objectMapper.writeValueAsString(response))
                    .expiresAt(Instant.now().plusSeconds(86400))
                    .build());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    static String hashBody(Object body, ObjectMapper objectMapper) {
        try {
            var json = body instanceof String s ? s : objectMapper.writeValueAsString(body);
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(json.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
