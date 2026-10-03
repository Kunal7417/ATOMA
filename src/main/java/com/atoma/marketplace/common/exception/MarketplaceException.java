package com.atoma.marketplace.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class MarketplaceException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Long retryAfterSeconds;
    private final Integer attemptsRemaining;

    public MarketplaceException(String message, HttpStatus status, String code) {
        this(message, status, code, null, null);
    }

    public MarketplaceException(
            String message,
            HttpStatus status,
            String code,
            Long retryAfterSeconds,
            Integer attemptsRemaining
    ) {
        super(message);
        this.status = status;
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
        this.attemptsRemaining = attemptsRemaining;
    }

    public static MarketplaceException of(
            HttpStatus status,
            String code,
            String message,
            Long retryAfterSeconds,
            Integer attemptsRemaining
    ) {
        return new MarketplaceException(message, status, code, retryAfterSeconds, attemptsRemaining);
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
        return new MarketplaceException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
