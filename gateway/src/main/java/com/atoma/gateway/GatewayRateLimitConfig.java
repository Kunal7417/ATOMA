package com.atoma.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayRateLimitConfig {

    @Bean
    KeyResolver deviceIdKeyResolver() {
        return exchange -> {
            var deviceId = exchange.getRequest().getHeaders().getFirst("X-Device-Id");
            if (deviceId != null && !deviceId.isBlank()) {
                return Mono.just(deviceId.trim());
            }
            var remote = exchange.getRequest().getRemoteAddress();
            if (remote != null && remote.getAddress() != null) {
                return Mono.just(remote.getAddress().getHostAddress());
            }
            return Mono.just("anonymous");
        };
    }
}
