package com.fulfillment.shippingservice.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.fulfillment.shippingservice.domain.exception.InvalidStatusTransitionException;
import static com.fulfillment.shippingservice.domain.shared.DomainValidations.requireNonBlank;

import lombok.Getter;

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
    private final String shippingGuideS3Key;

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
            Instant estimatedDeliveryAt,
            String shippingGuideS3Key) {
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
        this.estimatedDeliveryAt = Objects.requireNonNull(
                estimatedDeliveryAt,
                "estimatedDeliveryAt must not be null");
        this.shippingGuideS3Key = shippingGuideS3Key;
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
                estimatedDeliveryAt,
                null);
    }

    public static Shipment restore(
            String shipmentId,
            String orderId,
            String warehouseId,
            CarrierCode carrier,
            ShipmentStatus status,
            String trackingId,
            List<ShipmentItem> items,
            Instant createdAt,
            Instant shippedAt,
            Instant estimatedDeliveryAt,
            String shippingGuideS3Key) {
        return new Shipment(
                shipmentId,
                orderId,
                warehouseId,
                carrier,
                status,
                trackingId,
                items,
                createdAt,
                shippedAt,
                estimatedDeliveryAt,
                shippingGuideS3Key);
    }

    public Shipment withStatus(ShipmentStatus newStatus) {
        ShipmentStatus targetStatus = Objects.requireNonNull(newStatus, "newStatus must not be null");
        if (!this.status.canTransitionTo(targetStatus)) {
            throw new InvalidStatusTransitionException(this.status, targetStatus);
        }

        Instant nextShippedAt = this.shippedAt;
        if (targetStatus == ShipmentStatus.SHIPPED && this.shippedAt == null) {
            nextShippedAt = Instant.now();
        }

        return new Shipment(
                this.shipmentId,
                this.orderId,
                this.warehouseId,
                this.carrier,
                targetStatus,
                this.trackingId,
                this.items,
                this.createdAt,
                nextShippedAt,
                this.estimatedDeliveryAt,
                this.shippingGuideS3Key);
    }

    public Shipment withShippingGuideS3Key(String shippingGuideS3Key) {
        return new Shipment(
            this.shipmentId,
            this.orderId,
            this.warehouseId,
            this.carrier,
            this.status,
            this.trackingId,
            this.items,
            this.createdAt,
            this.shippedAt,
            this.estimatedDeliveryAt,
            requireNonBlank(shippingGuideS3Key, "shippingGuideS3Key"));
    }
}
