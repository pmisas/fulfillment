package com.fulfillment.orderservice.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static com.fulfillment.orderservice.domain.shared.DomainValidations.requireNonBlank;

import lombok.Getter;

@Getter
public class Order {

    private final String orderId;
    private final String operatorId;
    private final String warehouseId;
    private final Status status;
    private final double lat;
    private final double lng;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<OrderItem> items;
    

    private Order(
            String orderId,
            String operatorId,
            String warehouseId,
            Status status,
            Instant createdAt,
            Instant updatedAt,
            double lat,
            double lng,
            List<OrderItem> items) {
        this.orderId = requireNonBlank(orderId, "orderId");
        this.operatorId = requireNonBlank(operatorId, "operatorId");
        this.warehouseId = warehouseId;
        this.status = Objects.requireNonNull(status);
        this.lat = lat;
        this.lng = lng;
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.items = List.copyOf(Objects.requireNonNull(items));
        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
    }

    public static Order createOrder(
                String orderId,
                String operatorId,
                double lat,
                double lng,
                List<OrderItem> items) {
    Instant now = Instant.now();
    return new Order(
            orderId,
            operatorId,
            null,
            Status.RECEIVED,
            now,
            now,
            lat,
            lng,
            items
        );
    }  

    public static Order restore(
            String orderId,
            String operatorId,
            String warehouseId,
            Status status,
            Instant createdAt,
            Instant updatedAt,
            double lat,
            double lng,
            List<OrderItem> items) {
        return new Order(
            orderId, operatorId, warehouseId, status, createdAt, updatedAt, lat, lng, items);
    }

}
