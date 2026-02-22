package com.fulfillment.orderstateprocesor.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.fulfillment.orderstateprocesor.domain.exception.InvalidStatusTransitionException;

import static com.fulfillment.orderstateprocesor.domain.shared.DomainValidations.requireNonBlank;

public class Order {
    private final String orderId;
    private final String customerId;
    private final String warehouseId; // puede ser null/blank si aún no asignas
    private final Status status;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final List<OrderItem> items;

    private Order(
        String orderId,
        String customerId,
        String warehouseId,
        Status status,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItem> items
    ) {
        this.orderId = requireNonBlank(orderId, "orderId");
        this.customerId = requireNonBlank(customerId, "customerId");
        this.warehouseId = warehouseId == null ? "" : warehouseId.trim();
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.items = List.copyOf(Objects.requireNonNull(items, "items"));
        if (this.items.isEmpty()) throw new IllegalArgumentException("items must not be empty");
    }

    public static Order restore(
        String orderId,
        String customerId,
        String warehouseId,
        Status status,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItem> items
    ) {
        return new Order(orderId, customerId, warehouseId, status, createdAt, updatedAt, items);
    }

    public Order withStatus(Status newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(this.status, newStatus);
        }
        return new Order(orderId, customerId, warehouseId, newStatus, createdAt, Instant.now(), items);
    }

    public Order withWarehouse(String newWarehouseId) {
        String wh = requireNonBlank(newWarehouseId, "warehouseId").trim();
        return new Order(orderId, customerId, wh, status, createdAt, Instant.now(), items);
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getWarehouseId() { return warehouseId; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<OrderItem> getItems() { return items; }
}