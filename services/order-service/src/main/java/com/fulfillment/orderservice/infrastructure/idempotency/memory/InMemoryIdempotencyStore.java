package com.fulfillment.orderservice.infrastructure.idempotency.memory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fulfillment.orderservice.domain.ports.IdempotencyStore;

@Component
@Profile("local")
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private record Entry(String value, long expiresAtMs) {}

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(String key) {
        cleanupIfExpired(key);
        Entry e = store.get(key);
        return e == null ? Optional.empty() : Optional.of(e.value());
    }

    @Override
    public boolean claimPending(String key, String token, Duration ttl) {
        long exp = Instant.now().plus(ttl).toEpochMilli();
        String pending = "PENDING:" + token;

        cleanupIfExpired(key);

        return store.putIfAbsent(key, new Entry(pending, exp)) == null;
    }

    @Override
    public boolean finalizeOrderId(String key, String token, String orderId, Duration ttl) {
        long exp = Instant.now().plus(ttl).toEpochMilli();
        String expected = "PENDING:" + token;

        cleanupIfExpired(key);

        return store.computeIfPresent(key, (k, old) -> {
            if (!old.value().equals(expected)) return old;
            return new Entry(orderId, exp);
        }).value().equals(orderId);
    }

    @Override
    public boolean release(String key, String token) {
        String expected = "PENDING:" + token;

        cleanupIfExpired(key);

        return store.remove(key, new Entry(expected, 0))
               || store.computeIfPresent(key, (k, old) -> old.value().equals(expected) ? null : old) == null;
    }

    private void cleanupIfExpired(String key) {
        Entry e = store.get(key);
        if (e == null) return;
        if (e.expiresAtMs() <= Instant.now().toEpochMilli()) {
            store.remove(key, e);
        }
    }
}