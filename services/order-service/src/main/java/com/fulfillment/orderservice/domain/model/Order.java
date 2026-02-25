package com.fulfillment.orderservice.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.fulfillment.orderservice.domain.exception.InvalidStatusTransitionException;

import lombok.Getter;

@Getter
public class Order {

    private final String orderId;
    private final String werehouseId;
    private final Status status;
    private final double lat;
    private final double lng;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<OrderItem> items;
    

    private Order(
            String orderId,
            String werehouseId,
            Status status,
            Instant createdAt,
            Instant updatedAt,
            double lat,
            double lng,
            List<OrderItem> items) {
        this.orderId = orderId;
        this.werehouseId = werehouseId;
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
                double lat,
                double lng,
                List<OrderItem> items) {
    Instant now = Instant.now();
    return new Order(
            UUID.randomUUID().toString(),
            null,
            Status.RECEIVED,
            now,
            now,
            lat,
            lng,
            items
        );
    }

    

    public Order withStatus(Status newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(this.status, newStatus);
        }
        return new Order(
            this.orderId,
            this.werehouseId,
            Objects.requireNonNull(newStatus),
            this.createdAt,
            Instant.now(),
            this.lat,
            this.lng,
            this.items
        );
    }

    public static Order restore(
            String orderId,
            String werehouseId,
            Status status,
            Instant createdAt,
            Instant updatedAt,
            double lat,
            double lng,
            List<OrderItem> items) {
        return new Order(
            orderId, werehouseId, status, createdAt, updatedAt, lat, lng, items);
    }

}
