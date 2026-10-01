package com.atoma.marketplace.config;

import com.atoma.marketplace.auth.entity.User;
import com.atoma.marketplace.auth.repository.UserRepository;
import com.atoma.marketplace.common.enums.UserRole;
import com.atoma.marketplace.common.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Creates a fixed admin account for local/Postman testing when using the {@code dev} profile (H2).
 * Production and {@code prod} profile do not load this component.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DevAdminBootstrap implements ApplicationRunner {

    public static final String DEV_ADMIN_EMAIL = "admin@postman.local";
    public static final String DEV_ADMIN_PASSWORD = "Password1!";
    public static final String DEV_ADMIN_PHONE = "+9779800000001";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(DEV_ADMIN_EMAIL)) {
            return;
        }
        userRepository.save(User.builder()
                .email(DEV_ADMIN_EMAIL)
                .phone(DEV_ADMIN_PHONE)
                .passwordHash(passwordEncoder.encode(DEV_ADMIN_PASSWORD))
                .firstName("Postman")
                .lastName("Admin")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(UserRole.ADMIN))
                .build());
        log.info("Dev admin created for Postman: {} / {}", DEV_ADMIN_EMAIL, DEV_ADMIN_PASSWORD);
    }
}
