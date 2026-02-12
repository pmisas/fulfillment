package com.fulfillment.orderservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import static com.fulfillment.orderservice.domain.shared.DomainValidations.requireNonBlank;

public class Order {

    private final UUID orderId;
    private final String werehouse;
    private final String customerId;
    private final int totalItems;
    private final Status status;
    private final Instant createdAt;
    private final Instant updateAt;
    

    private Order(
            UUID orderId,
            String werehouse,
            String customerId,
            int total_items,
            Status status,
            Instant createdAt,
            Instant updateAt) {
        this.orderId = orderId;
        this.werehouse = requireNonBlank(werehouse, "werehouse");
        this.customerId = requireNonBlank(customerId, "customer_id");
        this.totalItems = total_items;
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updateAt = Objects.requireNonNull(updateAt);
    }

    public static Order createOrder(
                String werehouse,
                String customer_id,
                int totalItems) {
    Instant now = Instant.now();
    return new Order(
        UUID.randomUUID(),
        werehouse, 
        customer_id, 
        totalItems, 
        Status.CREATED,
        now,
        now
        );
    }

    public Order withStatus(Status newStatus) {
        return new Order(
            this.orderId,
            this.werehouse,
            this.customerId,
            this.totalItems,
            Objects.requireNonNull(newStatus),
            this.createdAt,
            Instant.now()
        );
    }

    
    public UUID getOrderId() {
        return orderId;
    }

    public String getWerehouse() {
        return werehouse;
    }

    public String getCustomerId() {
        return customerId;
    }

    public int getTotalItems() {
        return totalItems;
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

}
