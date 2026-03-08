package com.fulfillment.shippingservice.domain.model;

import java.util.Map;
import java.util.Set;

public enum ShipmentStatus {
    PENDING,
    SHIPPED,
    DELIVERED;

    private static final Map<ShipmentStatus, Set<ShipmentStatus>> ALLOWED = Map.of(
        PENDING, Set.of(SHIPPED),
        SHIPPED, Set.of(DELIVERED),
        DELIVERED, Set.of()
    );

    public boolean canTransitionTo(ShipmentStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }
}