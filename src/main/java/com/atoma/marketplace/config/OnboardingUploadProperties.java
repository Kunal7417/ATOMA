package com.atoma.marketplace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
@ConfigurationProperties(prefix = "atoma.onboarding.upload")
@Getter
@Setter
public class OnboardingUploadProperties {

    private long maxFileSizeBytes = 10_485_760L; // 10 MiB
    private Set<String> allowedContentTypes = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"
    );
    private Set<String> allowedKinds = Set.of(
            "BUSINESS_LICENCE",
            "TIN_CERTIFICATE",
            "TAZKIRA_FRONT",
            "TAZKIRA_BACK"
    );
}
