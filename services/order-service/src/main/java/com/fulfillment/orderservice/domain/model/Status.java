package com.fulfillment.orderservice.domain.model;

import java.util.Map;
import java.util.Set;

public enum Status {
    RECEIVED,
    VALIDATED,
    REJECTED,
    PICKED,
    PACKED,
    SHIPPED,
    DELIVERED,
    CANCELED;

    private static final Map<Status, Set<Status>> ALLOWED = Map.of(
        RECEIVED, Set.of(VALIDATED, REJECTED, CANCELED),
        VALIDATED, Set.of(PICKED, CANCELED),
        PICKED, Set.of(PACKED, CANCELED),
        PACKED, Set.of(SHIPPED),
        SHIPPED, Set.of(DELIVERED),
        DELIVERED, Set.of(),
        REJECTED, Set.of(),
        CANCELED, Set.of()
    );

    public boolean canTransitionTo(Status sig) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(sig);
    }
}
