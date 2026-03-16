package com.fulfillment.shippingservice.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fulfillment.shippingservice.domain.exception.InvalidStatusTransitionException;

class ShipmentTest {

    @Test
    void createShipment_shouldCreatePendingShipment() {
        Instant eta = Instant.now().plusSeconds(86400);

        Shipment shipment = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            eta
        );

        assertEquals("ship-1", shipment.getShipmentId());
        assertEquals("order-1", shipment.getOrderId());
        assertEquals("wh-1", shipment.getWarehouseId());
        assertEquals(CarrierCode.INTERNAL_CARRIER, shipment.getCarrier());
        assertEquals(ShipmentStatus.PENDING, shipment.getStatus());
        assertNull(shipment.getTrackingId());
        assertNotNull(shipment.getCreatedAt());
        assertNull(shipment.getShippedAt());
        assertEquals(eta, shipment.getEstimatedDeliveryAt());
        assertNull(shipment.getShippingGuideS3Key());
    }

    @Test
    void createShipment_shouldThrowWhenItemsAreEmpty() {
        Instant eta = Instant.now().plusSeconds(86400);

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> Shipment.createShipment(
                "ship-1",
                "order-1",
                "wh-1",
                CarrierCode.INTERNAL_CARRIER,
                List.of(),
                eta
            )
        );

        assertEquals("items must not be empty", ex.getMessage());
    }

    @Test
    void withTrackingId_shouldReturnUpdatedShipment() {
        Shipment shipment = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        );

        Shipment updated = shipment.withTrackingId("trk-123");

        assertEquals("trk-123", updated.getTrackingId());
        assertEquals(shipment.getShipmentId(), updated.getShipmentId());
    }

    @Test
    void withStatus_shouldAllowValidTransitionPendingToShipped() {
        Shipment shipment = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        );

        Shipment updated = shipment.withStatus(ShipmentStatus.SHIPPED);

        assertEquals(ShipmentStatus.SHIPPED, updated.getStatus());
        assertNotNull(updated.getShippedAt());
    }

    @Test
    void withStatus_shouldAllowValidTransitionShippedToDelivered() {
        Shipment shipment = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        ).withStatus(ShipmentStatus.SHIPPED);

        Shipment updated = shipment.withStatus(ShipmentStatus.DELIVERED);

        assertEquals(ShipmentStatus.DELIVERED, updated.getStatus());
        assertEquals(shipment.getShippedAt(), updated.getShippedAt());
    }

    @Test
    void withStatus_shouldThrowWhenTransitionIsInvalid() {
        Shipment shipment = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        );

        assertThrows(
            InvalidStatusTransitionException.class,
            () -> shipment.withStatus(ShipmentStatus.DELIVERED)
        );
    }

    @Test
    void withShippingGuideS3Key_shouldReturnUpdatedShipment() {
        Shipment shipment = Shipment.createShipment(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            Instant.now().plusSeconds(86400)
        );

        Shipment updated = shipment.withShippingGuideS3Key("shipments/ship-1/guide.pdf");

        assertEquals("shipments/ship-1/guide.pdf", updated.getShippingGuideS3Key());
    }

    @Test
    void restore_shouldRestoreShipmentCorrectly() {
        Instant createdAt = Instant.now();
        Instant shippedAt = createdAt.plusSeconds(3600);
        Instant eta = createdAt.plusSeconds(86400);

        Shipment shipment = Shipment.restore(
            "ship-1",
            "order-1",
            "wh-1",
            CarrierCode.INTERNAL_CARRIER,
            ShipmentStatus.SHIPPED,
            "trk-123",
            List.of(ShipmentItem.createShipmentItem("SKU-1", 2)),
            createdAt,
            shippedAt,
            eta,
            "shipments/ship-1/guide.pdf"
        );

        assertEquals("ship-1", shipment.getShipmentId());
        assertEquals("order-1", shipment.getOrderId());
        assertEquals("wh-1", shipment.getWarehouseId());
        assertEquals(ShipmentStatus.SHIPPED, shipment.getStatus());
        assertEquals("trk-123", shipment.getTrackingId());
        assertEquals(createdAt, shipment.getCreatedAt());
        assertEquals(shippedAt, shipment.getShippedAt());
        assertEquals(eta, shipment.getEstimatedDeliveryAt());
        assertEquals("shipments/ship-1/guide.pdf", shipment.getShippingGuideS3Key());
    }
}
