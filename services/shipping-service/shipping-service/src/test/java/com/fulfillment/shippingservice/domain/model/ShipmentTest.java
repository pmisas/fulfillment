package com.fulfillment.shippingservice.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fulfillment.shippingservice.domain.exception.InvalidStatusTransitionException;

class ShipmentTest {

    @Test
    void createShipment_whenValid_shouldCreatePendingShipment() {
        Shipment shipment = Shipment.createShipment(
                "ship-1",
                "order-1",
                "wh-1",
                CarrierCode.INTERNAL_CARRIER,
                List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
                Instant.now().plusSeconds(86400));

        assertEquals("ship-1", shipment.getShipmentId());
        assertEquals(ShipmentStatus.PENDING, shipment.getStatus());
        assertNotNull(shipment.getCreatedAt());
    }

    @Test
    void withStatus_whenInvalidTransition_shouldThrow() {
        Shipment shipment = Shipment.createShipment(
                "ship-1",
                "order-1",
                "wh-1",
                CarrierCode.INTERNAL_CARRIER,
                List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
                Instant.now().plusSeconds(86400));

        assertThrows(InvalidStatusTransitionException.class,
                () -> shipment.withStatus(ShipmentStatus.DELIVERED));
    }

    @Test
    void withStatus_whenShipped_shouldSetShippedAt() {
        Shipment shipment = Shipment.createShipment(
                "ship-1",
                "order-1",
                "wh-1",
                CarrierCode.INTERNAL_CARRIER,
                List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
                Instant.now().plusSeconds(86400))
                .withTrackingId("trk-123")
                .withStatus(ShipmentStatus.SHIPPED);

        assertEquals(ShipmentStatus.SHIPPED, shipment.getStatus());
        assertNotNull(shipment.getShippedAt());
    }
}
