package com.fulfillment.orderstateprocesor.domain.model;

import java.util.Map;
import java.util.Set;

public enum Status {
    RECEIVED,
    VALIDATED,
    REJECTED,
    PACKED,
    SHIPPED,
    CANCELED;

    private static final Map<Status, Set<Status>> ALLOWED = Map.of(
        RECEIVED, Set.of(VALIDATED, REJECTED, CANCELED),
        VALIDATED, Set.of(PACKED, CANCELED),
        PACKED, Set.of(SHIPPED),
        SHIPPED, Set.of(),
        REJECTED, Set.of(),
        CANCELED, Set.of()
    );

    public boolean canTransitionTo(Status next) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(next);
    }
}