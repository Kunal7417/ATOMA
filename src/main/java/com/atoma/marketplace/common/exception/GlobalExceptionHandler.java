package com.atoma.marketplace.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MarketplaceException.class)
    public ResponseEntity<ErrorResponse> handleMarketplaceException(
            MarketplaceException ex,
            HttpServletRequest request
    ) {
        return buildResponse(
                ex.getStatus(),
                ex.getCode(),
                ex.getMessage(),
                ex.getDetails(),
                ex.getRetryAfterSeconds(),
                ex.getAttemptsRemaining()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ErrorCodes.VALIDATION_FAILED,
                "Validation failed",
                Map.of("fields", fields),
                null,
                null
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, ex.getMessage(),
                null, null, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(),
                null, null, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_ERROR, ex.getMessage(),
                null, null, null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            Map<String, Object> extraDetails,
            Long retryAfter,
            Integer attemptsLeft
    ) {
        var details = new HashMap<String, Object>();
        if (extraDetails != null) {
            details.putAll(extraDetails);
        }
        if (retryAfter != null) {
            details.put("retryAfter", retryAfter);
        }
        if (attemptsLeft != null) {
            details.put("attemptsLeft", attemptsLeft);
        }

        var body = ErrorResponse.builder()
                .code(code)
                .message(message)
                .details(details.isEmpty() ? Map.of() : Map.copyOf(details))
                .build();

        var response = ResponseEntity.status(status).body(body);
        if (retryAfter != null
                && (ErrorCodes.RESEND_TOO_SOON.equals(code) || ErrorCodes.RATE_LIMITED.equals(code))) {
            return ResponseEntity.status(status)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter))
                    .body(body);
        }
        return response;
    }
}
