package com.fulfillment.orderstateprocesor.domain.model;

import java.util.Map;
import java.util.Set;

public enum Status {
    RECEIVED,
    VALIDATED,
    REJECTED,
    PICKED,
    PACKED,
    SHIPPED,
    CANCELED;

    private static final Map<Status, Set<Status>> ALLOWED = Map.of(
        RECEIVED, Set.of(VALIDATED, REJECTED, CANCELED),
        VALIDATED, Set.of(PICKED, CANCELED),
        PICKED, Set.of(PACKED, CANCELED),
        PACKED, Set.of(SHIPPED, CANCELED),
        SHIPPED, Set.of(),
        REJECTED, Set.of(),
        CANCELED, Set.of()
    );

    public boolean canTransitionTo(Status next) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(next);
    }
}

