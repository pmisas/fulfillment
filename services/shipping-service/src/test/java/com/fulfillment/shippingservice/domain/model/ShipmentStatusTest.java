package com.fulfillment.shippingservice.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ShipmentStatusTest {

    @Test
    void pending_shouldAllowOnlyShipped() {
        assertTrue(ShipmentStatus.PENDING.canTransitionTo(ShipmentStatus.SHIPPED));
        assertFalse(ShipmentStatus.PENDING.canTransitionTo(ShipmentStatus.DELIVERED));
        assertFalse(ShipmentStatus.PENDING.canTransitionTo(ShipmentStatus.PENDING));
    }

    @Test
    void shipped_shouldAllowOnlyDelivered() {
        assertTrue(ShipmentStatus.SHIPPED.canTransitionTo(ShipmentStatus.DELIVERED));
        assertFalse(ShipmentStatus.SHIPPED.canTransitionTo(ShipmentStatus.PENDING));
        assertFalse(ShipmentStatus.SHIPPED.canTransitionTo(ShipmentStatus.SHIPPED));
    }

    @Test
    void delivered_shouldAllowNoTransitions() {
        for (ShipmentStatus target : ShipmentStatus.values()) {
            assertFalse(ShipmentStatus.DELIVERED.canTransitionTo(target));
        }
    }
}
