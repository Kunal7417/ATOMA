package com.atoma.marketplace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "atoma.otp")
@Getter
@Setter
public class OtpProperties {

    private int length = 6;
    private int ttlMinutes = 5;
    private int maxAttemptsPerHour = 10;
    private int maxWrongAttempts = 5;
    private int resendCooldownSeconds = 60;
    /** When true, send-code response includes the OTP (dev/test only). */
    private boolean exposeCode = false;
    /** Fixed code accepted in dev/test when exposeCode is true. */
    private String devBypassCode = "000000";
}
