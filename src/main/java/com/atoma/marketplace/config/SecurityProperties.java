package com.atoma.marketplace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "atoma.security")
@Getter
@Setter
public class SecurityProperties {

    private String jwtSecret = "change-me-in-production-use-at-least-256-bit-secret-key";
    /** Legacy / non-mobile JWT TTL (e.g. admin web). */
    private long jwtExpirationMs = 86400000L;
    /** Merchant mobile access token TTL (15 minutes). */
    private long merchantAccessTokenExpirationMs = 900_000L;
    private String[] publicPaths = {
            "/api/v1/auth/**",
            "/api/v1/catalog/**",
            "/api/v1/health",
            "/actuator/health",
            "/h2-console/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };
}
