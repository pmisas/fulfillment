package com.fulfillment.orderservice.domain.ports;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStore {
    Optional<String> getOrderId(String Key);
    boolean putIfAbsent(String key, String orderId, Duration ttl);
}
