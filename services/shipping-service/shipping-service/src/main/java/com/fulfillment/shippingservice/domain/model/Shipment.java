package com.example.shipping.model;

import static com.fulfillment.orderservice.domain.shared.DomainValidations.requireNonBlank;

import java.time.Instant;
import java.util.List;

@Getter
public class Shipment {

    private final String shipmentId;
    private final String orderId;
    private final String warehouseId;
    private final CarrierCode carrier;
    private final ShipmentStatus status;
    private final String trackingId;
    private final List<ShipmentItem> items;
    private final Instant createdAt;
    private final Instant shippedAt;
    private final Instant estimatedDeliveryAt;

    private Shipment(
            String shipmentId,
            String orderId,
            String warehouseId,
            CarrierCode carrier,
            ShipmentStatus status,
            String trackingId,
            List<ShipmentItem> items,
            Instant createdAt,
            Instant shippedAt,
            Instant estimatedDeliveryAt) {
        this.shipmentId = requireNonBlank(shipmentId, "shipmentId");
        this.orderId = requireNonBlank(orderId, "orderId");
        this.warehouseId = requireNonBlank(warehouseId, "warehouseId");
        this.carrier = Objects.requireNonNull(carrier, "carrier must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.trackingId = trackingId;

        this.items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }

        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.shippedAt = shippedAt;
        this.estimatedDeliveryAt = Objects.requireNonNull(estimatedDeliveryAt, "estimatedDeliveryAt must not be null");
    }

    public static Shipment createShipment(
            String shipmentId,
            String orderId,
            String warehouseId,
            CarrierCode carrier,
            List<ShipmentItem> items,
            Instant estimatedDeliveryAt) {
        Instant now = Instant.now();
        return new Shipment(
            shipmentId,
            orderId,
            warehouseId,
            carrier,
            ShipmentStatus.PENDING,
            null,
            items,
            now,
            null,
            estimatedDeliveryAt
        );
    }

    public Shipment withStatus(ShipmentStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(this.status, newStatus);
        }
        return new Shipment(
            this.shipmentId,
            this.orderId,
            this.warehouseId,
            this.carrier,
            Objects.requireNonNull(newStatus),
            this.trackingId,
            this.items,
            this.createdAt,
            Instant.now(),
            this.estimatedDeliveryAt
        );
    }

}
