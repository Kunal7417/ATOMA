package com.atoma.marketplace.config;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** devCode is only exposed in dev/local/test — never staging-with-real-users or production. */
@Component
@RequiredArgsConstructor
public class OtpExposeCodePolicy {

    private final Environment environment;
    private final OtpProperties otpProperties;

    public boolean includeDevCodeInResponse() {
        if (!otpProperties.isExposeCode()) {
            return false;
        }
        var allowed = Arrays.asList(environment.getActiveProfiles());
        if (allowed.isEmpty()) {
            allowed = Arrays.asList("default");
        }
        return allowed.stream().anyMatch(p ->
                p.equals("dev") || p.equals("local") || p.equals("test"));
    }
}
