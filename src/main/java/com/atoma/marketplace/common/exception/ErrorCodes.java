package com.atoma.marketplace.common.exception;

public final class ErrorCodes {

    private ErrorCodes() {}

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String OTP_INVALID = "OTP_INVALID";
    public static final String OTP_EXPIRED = "OTP_EXPIRED";
    public static final String OTP_ALREADY_USED = "OTP_ALREADY_USED";
    public static final String OTP_REQUEST_NOT_FOUND = "OTP_REQUEST_NOT_FOUND";
    public static final String OTP_TOO_MANY_ATTEMPTS = "OTP_TOO_MANY_ATTEMPTS";
    public static final String RESEND_TOO_SOON = "RESEND_TOO_SOON";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String REFRESH_TOKEN_INVALID = "REFRESH_TOKEN_INVALID";
    public static final String VERSION_CONFLICT = "VERSION_CONFLICT";
    public static final String APPLICATION_LOCKED = "APPLICATION_LOCKED";
    public static final String APP_UPDATE_REQUIRED = "APP_UPDATE_REQUIRED";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
}
