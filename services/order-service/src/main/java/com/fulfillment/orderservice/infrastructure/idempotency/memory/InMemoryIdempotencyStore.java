package com.fulfillment.orderservice.infrastructure.idempotency.memory;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.fulfillment.orderservice.domain.ports.IdempotencyStore;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("local")
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final Map<String, String> store = new ConcurrentHashMap<>();

    @Override
    public Optional<String> getOrderId(String key) {
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public boolean putIfAbsent(String key, String orderId, Duration ttl) {
        return store.putIfAbsent(key, orderId) == null;
    }
}
