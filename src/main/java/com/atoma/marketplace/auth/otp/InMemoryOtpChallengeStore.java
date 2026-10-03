package com.atoma.marketplace.auth.otp;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryOtpChallengeStore implements OtpChallengeStore {

    private final Map<UUID, OtpChallenge> byId = new ConcurrentHashMap<>();
    private final Map<String, List<Instant>> sendLog = new ConcurrentHashMap<>();

    @Override
    public void save(OtpChallenge challenge) {
        byId.put(challenge.getRequestId(), challenge);
        sendLog.compute(destinationKey(challenge), (k, list) -> {
            var times = list == null ? new ArrayList<Instant>() : new ArrayList<>(list);
            times.add(challenge.getLastSentAt());
            var cutoff = Instant.now().minusSeconds(3600);
            times.removeIf(t -> t.isBefore(cutoff));
            return times;
        });
    }

    @Override
    public Optional<OtpChallenge> findById(UUID requestId) {
        return Optional.ofNullable(byId.get(requestId));
    }

    @Override
    public void markSuperseded(UUID requestId) {
        byId.computeIfPresent(requestId, (id, c) -> OtpChallenge.builder()
                .requestId(c.getRequestId())
                .channel(c.getChannel())
                .destination(c.getDestination())
                .code(c.getCode())
                .expiresAt(c.getExpiresAt())
                .resendAllowedAt(c.getResendAllowedAt())
                .consumed(c.isConsumed())
                .superseded(true)
                .wrongAttempts(c.getWrongAttempts())
                .lastSentAt(c.getLastSentAt())
                .build());
    }

    @Override
    public void markConsumed(UUID requestId) {
        byId.computeIfPresent(requestId, (id, c) -> OtpChallenge.builder()
                .requestId(c.getRequestId())
                .channel(c.getChannel())
                .destination(c.getDestination())
                .code(c.getCode())
                .expiresAt(c.getExpiresAt())
                .resendAllowedAt(c.getResendAllowedAt())
                .consumed(true)
                .superseded(c.isSuperseded())
                .wrongAttempts(c.getWrongAttempts())
                .lastSentAt(c.getLastSentAt())
                .build());
    }

    @Override
    public void incrementWrongAttempts(UUID requestId) {
        byId.computeIfPresent(requestId, (id, c) -> OtpChallenge.builder()
                .requestId(c.getRequestId())
                .channel(c.getChannel())
                .destination(c.getDestination())
                .code(c.getCode())
                .expiresAt(c.getExpiresAt())
                .resendAllowedAt(c.getResendAllowedAt())
                .consumed(c.isConsumed())
                .superseded(c.isSuperseded())
                .wrongAttempts(c.getWrongAttempts() + 1)
                .lastSentAt(c.getLastSentAt())
                .build());
    }

    @Override
    public int countSendsForDestinationSince(String destinationKey, Instant since) {
        var list = sendLog.get(destinationKey);
        if (list == null) {
            return 0;
        }
        return (int) list.stream().filter(t -> !t.isBefore(since)).count();
    }

    private static String destinationKey(OtpChallenge challenge) {
        return challenge.getChannel().name() + ":" + challenge.getDestination().toLowerCase();
    }
}
