package com.fulfillment.orderstateprocesor.domain.ports;

import java.time.Duration;

public interface ProcessedEventStore {
    boolean putIfAbsent(String eventId, Duration ttl);
}
