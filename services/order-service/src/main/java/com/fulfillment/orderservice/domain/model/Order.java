package com.fulfillment.orderservice.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.fulfillment.orderservice.domain.shared.DomainValidations.requireNonBlank;

public class Order {

    private final String orderId;
    private final String werehouse;
    private final String customerId;
    private final Status status;
    private final Instant createdAt;
    private final Instant updateAt;
    private final List<OrderItem> items;
    

    private Order(
            String orderId,
            String werehouse,
            String customerId,
            Status status,
            Instant createdAt,
            Instant updateAt,
            List<OrderItem> items) {
        this.orderId = orderId;
        this.werehouse = requireNonBlank(werehouse, "werehouse");
        this.customerId = requireNonBlank(customerId, "customer_id");
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updateAt = Objects.requireNonNull(updateAt);
        this.items = List.copyOf(Objects.requireNonNull(items));
        if (this.items.isEmpty()) {
            throw new IllegalArgumentException("items must not be empty");
        }
    }

    public static Order createOrder(
                String werehouse,
                String customer_id,
                List<OrderItem> items) {
    Instant now = Instant.now();
    return new Order(
        UUID.randomUUID().toString(),
        werehouse, 
        customer_id,
        Status.CREATED,
        now,
        now,
        items
        );
    }

    public Order withStatus(Status newStatus) {
        return new Order(
            this.orderId,
            this.werehouse,
            this.customerId,
            Objects.requireNonNull(newStatus),
            this.createdAt,
            Instant.now(),
            this.items
        );
    }

    
    public String getOrderId() {
        return orderId;
    }

    public String getWerehouse() {
        return werehouse;
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
        return updateAt;
    }

    public List<OrderItem> getItems() {
        return items;
    }

}
