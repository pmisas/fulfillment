package com.fulfillment.orderservice.domain.ports;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStore {
    Optional<String> get(String key);
    boolean claimPending(String key, String token, Duration ttl);
    boolean finalizeOrderId(String key, String token, String orderId, Duration ttl);
    boolean release(String key, String token);
}
