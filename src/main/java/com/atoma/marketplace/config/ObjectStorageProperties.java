package com.atoma.marketplace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "atoma.storage")
@Getter
@Setter
public class ObjectStorageProperties {

    private String endpoint = "http://localhost:9000";
    private String accessKey = "atoma";
    private String secretKey = "atoma-secret-key";
    private String bucket = "atoma-marketplace";
}
