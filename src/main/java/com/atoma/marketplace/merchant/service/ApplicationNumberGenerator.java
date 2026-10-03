package com.atoma.marketplace.merchant.service;

import com.atoma.marketplace.merchant.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class ApplicationNumberGenerator {

    private static final DateTimeFormatter YYMM = DateTimeFormatter.ofPattern("yyMM");

    private final MerchantRepository merchantRepository;

    public String generate() {
        var prefix = "APP-" + YearMonth.now().format(YYMM) + "-";
        for (int attempt = 0; attempt < 20; attempt++) {
            var candidate = prefix + String.format("%04d", ThreadLocalRandom.current().nextInt(10_000));
            if (merchantRepository.findByApplicationNumber(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate unique application number");
    }
}
