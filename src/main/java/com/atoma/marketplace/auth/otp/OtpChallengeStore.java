package com.atoma.marketplace.auth.otp;

import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeStore {

    void save(OtpChallenge challenge);

    Optional<OtpChallenge> findById(UUID requestId);

    void markSuperseded(UUID requestId);

    void markConsumed(UUID requestId);

    void incrementWrongAttempts(UUID requestId);

    int countSendsForDestinationSince(String destinationKey, java.time.Instant since);
}
