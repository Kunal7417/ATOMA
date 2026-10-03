package com.atoma.marketplace.auth.otp;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryOtpStore implements OtpStore {

    private record Entry(String code, Instant expiresAt) {}

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public void save(String key, String code, Duration ttl) {
        store.put(key, new Entry(code, Instant.now().plus(ttl)));
    }

    @Override
    public Optional<String> get(String key) {
        var entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.code());
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }
}
