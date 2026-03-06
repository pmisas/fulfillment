package com.fulfillment.shippingservice.domain.model;

import java.util.Map;
import java.util.Set;

public enum ShipmentStatus {
    CREATED,
    SHIPPED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED;

    
    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED = Map.of(
        CREATED, Set.of(SHIPPED, CANCELLED),
        SHIPPED, Set.of(IN_TRANSIT, CANCELLED),
        IN_TRANSIT, Set.of(DELIVERED, CANCELLED),
        DELIVERED, Set.of(),
        CANCELLED, Set.of()
    );

    public boolean canTransitionTo(ShipmentStatus sig) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(sig);
    }
}