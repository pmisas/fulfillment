package com.fulfillment.orderstateprocesor.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.fulfillment.orderstateprocesor.domain.exception.InvalidStatusTransitionException;

import static com.fulfillment.orderstateprocesor.domain.shared.DomainValidations.requireNonBlank;

import lombok.Getter;

@Getter
public class Order {

    private final String orderId;
    private final String warehouseId;
    private final Status status;
    private final double lat;
    private final double lng;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<OrderItem> items;
    

       private Order(
        String orderId,
        String warehouseId,
        Status status,
        double lat,
        double lng,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItem> items
    ) {
        this.orderId = requireNonBlank(orderId, "orderId");

        this.warehouseId = (warehouseId == null || warehouseId.isBlank())
                ? null
                : warehouseId.trim();

        this.status = Objects.requireNonNull(status, "status");
        this.lat = lat;
        this.lng = lng;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");

        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (this.items.isEmpty()) throw new IllegalArgumentException("items must not be empty");
    }
     

    public static Order createOrder(
                String orderId,
                double lat,
                double lng,
                List<OrderItem> items) {
    Instant now = Instant.now();
    return new Order(
            orderId,
            null,
            Status.RECEIVED,
            lat,
            lng,
            now,
            now,
            items
        );
    }  

    public Order withStatus(Status newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(this.status, newStatus);
        }
        return new Order(
            this.orderId,
            this.warehouseId,
            Objects.requireNonNull(newStatus),
            this.lat,
            this.lng,
            this.createdAt,
            Instant.now(),
            this.items
        );
    }
     public Order withWarehouse(String warehouseId) {
        return new Order(
            this.orderId,
            warehouseId,
            this.status,
            this.lat,
            this.lng,
            this.createdAt,
            Instant.now(),
            this.items
        ); 
    }
    
    public static Order restore(
            String orderId,
            String warehouseId,
            Status status,
            double lat,
            double lng,
            Instant createdAt,
            Instant updatedAt,
            List<OrderItem> items) {
        return new Order(
            orderId, warehouseId, status, lat, lng, createdAt, updatedAt, items);
    }

}
