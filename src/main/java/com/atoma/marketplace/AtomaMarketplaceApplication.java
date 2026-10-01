package com.atoma.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AtomaMarketplaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AtomaMarketplaceApplication.class, args);
    }
}
