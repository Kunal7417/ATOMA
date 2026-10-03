package com.atoma.marketplace.auth.otp;

import com.atoma.marketplace.common.enums.OtpDeliveryChannel;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class OtpChallenge {
    UUID requestId;
    OtpDeliveryChannel channel;
    String destination;
    String code;
    Instant expiresAt;
    Instant resendAllowedAt;
    boolean consumed;
    boolean superseded;
    int wrongAttempts;
    Instant lastSentAt;
}
