package com.atoma.marketplace.common.exception;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder
public class ErrorResponse {
    Instant timestamp;
    int status;
    String code;
    String message;
    String path;
    Map<String, String> fieldErrors;
    /** Seconds until client may retry (rate limit / resend cooldown). */
    Long retryAfter;
    /** Wrong OTP attempts remaining, when applicable. */
    Integer attemptsRemaining;
}
