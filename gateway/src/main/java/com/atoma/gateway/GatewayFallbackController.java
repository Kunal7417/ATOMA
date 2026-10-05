package com.atoma.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class GatewayFallbackController {

    @Value("${atoma.gateway.fallback-message:Service unavailable}")
    private String fallbackMessage;

    @GetMapping("/fallback")
    public Mono<Void> fallback(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        var body = Map.of(
                "code", "SERVICE_UNAVAILABLE",
                "message", fallbackMessage,
                "details", Map.of()
        );
        var bytes = Mono.fromCallable(() ->
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(body));
        return bytes.flatMap(data -> exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(data))));
    }
}
