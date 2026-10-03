package com.atoma.marketplace.auth.otp;

import java.time.Duration;
import java.util.Optional;

public interface OtpStore {

    void save(String key, String code, Duration ttl);

    Optional<String> get(String key);

    void delete(String key);
}
