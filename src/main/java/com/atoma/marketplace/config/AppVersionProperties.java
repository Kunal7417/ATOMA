package com.atoma.marketplace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "atoma.app")
@Getter
@Setter
public class AppVersionProperties {

    /** Minimum merchant app version (semver x.y.z). Requests with lower X-App-Version receive 426. */
    private String minMerchantVersion = "0.1.0";
    /** When false, skip version check (e.g. curl without header). */
    private boolean enforceVersionHeader = false;
}
