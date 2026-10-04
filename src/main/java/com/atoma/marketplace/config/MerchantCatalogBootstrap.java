package com.atoma.marketplace.config;

import com.atoma.marketplace.product.entity.Category;
import com.atoma.marketplace.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds standard merchant categories when Flyway is off (dev/test H2).
 * PostgreSQL uses {@code V5__merchant_onboarding_contract.sql}.
 */
@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
@Slf4j
public class MerchantCatalogBootstrap implements ApplicationRunner {

    private static final String[][] CATALOG = {
            {"Food & grocery", "food-grocery", "1"},
            {"Electronics", "electronics", "2"},
            {"Pharmacy", "pharmacy", "3"},
            {"Fashion", "fashion", "4"},
            {"Home & kitchen", "home-kitchen", "5"},
            {"Beauty", "beauty", "6"},
            {"Handicrafts & carpets", "handicrafts-carpets", "7"},
    };

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (var row : CATALOG) {
            if (categoryRepository.existsBySlug(row[1])) {
                continue;
            }
            categoryRepository.save(Category.builder()
                    .name(row[0])
                    .slug(row[1])
                    .active(true)
                    .sortOrder(Integer.parseInt(row[2]))
                    .build());
        }
    }
}
