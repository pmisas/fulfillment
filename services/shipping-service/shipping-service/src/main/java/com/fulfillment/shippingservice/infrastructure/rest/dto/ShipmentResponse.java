package com.fulfillment.shippingservice.infrastructure.rest.dto;

import java.time.Instant;
import java.util.List;

public record ShipmentResponse(
        String shipmentId,
        String orderId,
        String warehouseId,
        String carrier,
        String status,
        String trackingId,
        List<Item> items,
        Instant createdAt,
        Instant shippedAt,
        Instant estimatedDeliveryAt) {

    public record Item(String sku, int quantity) {
    }
}
