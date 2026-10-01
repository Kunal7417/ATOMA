package com.atoma.marketplace.common.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Platform", description = "Platform health and metadata")
public class PlatformController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "platform", "ATOMA Pay Marketplace",
                "architecture", "Spring Boot Monolith",
                "surfaces", java.util.List.of(
                        "Merchant Mobile App",
                        "Merchant Web Portal",
                        "Admin Portal",
                        "Customer Website",
                        "Customer Mobile App"
                )
        );
    }
}
