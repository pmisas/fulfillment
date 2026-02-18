package com.fulfillment.orderservice.domain.model;

import java.util.Map;
import java.util.Set;

public enum Status {
    CREATED,
    CONFIRMED,
    SHIPPED,
    CANCELED,
    PACKED;

    private static final Map<Status, Set<Status>> ALLOWED = Map.of(
        CREATED, Set.of(CONFIRMED, CANCELED),
        CONFIRMED, Set.of(PACKED, CANCELED),
        PACKED, Set.of(SHIPPED),
        SHIPPED, Set.of(),
        CANCELED, Set.of()
    );

    public boolean canTransitionTo(Status sig) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(sig);
    }
}
