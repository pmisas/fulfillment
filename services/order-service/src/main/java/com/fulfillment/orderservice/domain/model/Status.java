package com.fulfillment.orderservice.domain.model;

import java.util.Map;
import java.util.Set;

public enum Status {
    RECEIVED,
    VALIDATED,
    SHIPPED,
    CANCELED,
    PACKED;

    private static final Map<Status, Set<Status>> ALLOWED = Map.of(
        RECEIVED, Set.of(VALIDATED, CANCELED),
        VALIDATED, Set.of(PACKED, CANCELED),
        PACKED, Set.of(SHIPPED),
        SHIPPED, Set.of(),
        CANCELED, Set.of()
    );

    public boolean canTransitionTo(Status sig) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(sig);
    }
}
