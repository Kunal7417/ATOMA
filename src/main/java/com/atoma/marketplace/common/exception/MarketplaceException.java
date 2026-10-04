package com.atoma.marketplace.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class MarketplaceException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Long retryAfterSeconds;
    private final Integer attemptsRemaining;
    private final Map<String, Object> details;

    public MarketplaceException(String message, HttpStatus status, String code) {
        this(message, status, code, null, null, null);
    }

    public MarketplaceException(
            String message,
            HttpStatus status,
            String code,
            Long retryAfterSeconds,
            Integer attemptsRemaining
    ) {
        this(message, status, code, retryAfterSeconds, attemptsRemaining, null);
    }

    public MarketplaceException(
            String message,
            HttpStatus status,
            String code,
            Long retryAfterSeconds,
            Integer attemptsRemaining,
            Map<String, Object> details
    ) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
        this.attemptsRemaining = attemptsRemaining;
        this.details = details;
    }

    public static MarketplaceException of(
            HttpStatus status,
            String code,
            String message,
            Long retryAfterSeconds,
            Integer attemptsRemaining
    ) {
        return new MarketplaceException(message, status, code, retryAfterSeconds, attemptsRemaining, null);
    }

    public static MarketplaceException withDetails(
            HttpStatus status,
            String code,
            String message,
            Map<String, Object> details
    ) {
        return new MarketplaceException(message, status, code, null, null, details);
    }

    public static MarketplaceException notFound(String resource, Object id) {
        return new MarketplaceException(
                resource + " not found: " + id,
                HttpStatus.NOT_FOUND,
                "NOT_FOUND"
        );
    }

    public static MarketplaceException badRequest(String message) {
        return new MarketplaceException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static MarketplaceException forbidden(String message) {
        return new MarketplaceException(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    public static MarketplaceException conflict(String message) {
        return new MarketplaceException(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public static MarketplaceException unauthorized(String message) {
        return new MarketplaceException(message, HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED);
    }
}
