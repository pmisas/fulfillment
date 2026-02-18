package com.fulfillment.orderservice.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.fulfillment.orderservice.domain.shared.DomainValidations.requireNonBlank;

public class Order {

    private final String orderId;
    private final String werehouseId;
    private final String customerId;
    private final Status status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<OrderItem> items;
    

    private Order(
            String orderId,
            String werehouseId,
            String customerId,
            Status status,
            Instant createdAt,
            Instant updatedAt,
            List<OrderItem> items) {
        this.orderId = orderId;
        this.werehouseId = requireNonBlank(werehouseId, "werehouse");
        this.customerId = requireNonBlank(customerId, "customer_id");
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        this.items = List.copyOf(Objects.requireNonNull(items));
        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
    }

    public static Order createOrder(
                String werehouseId,
                String customerId,
                List<OrderItem> items) {
    Instant now = Instant.now();
    return new Order(
        UUID.randomUUID().toString(),
        werehouseId, 
        customerId,
        Status.CREATED,
        now,
        now,
        items
        );
    }

    public Order withStatus(Status newStatus) {
        return new Order(
            this.orderId,
            this.werehouseId,
            this.customerId,
            Objects.requireNonNull(newStatus),
            this.createdAt,
            Instant.now(),
            this.items
        );
    }

    public static Order restore(
            String orderId,
            String werehouseId,
            String customerId,
            Status status,
            Instant createdAt,
            Instant updatedAt,
            List<OrderItem> items
    ) {
        return new Order(
            orderId, werehouseId, customerId, status, createdAt, updatedAt, items);
    }

    
    public String getOrderId() {
        return orderId;
    }

    public String getWerehouseId() {
        return werehouseId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Status getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

}
