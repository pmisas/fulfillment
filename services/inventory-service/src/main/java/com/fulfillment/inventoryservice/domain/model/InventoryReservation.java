package com.fulfillment.inventoryservice.domain.model;

import static com.fulfillment.inventoryservice.domain.shared.DomainValidations.requireNonBlank;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

@Getter
public class InventoryReservation {
    private final String reservationId;
    private final String orderId;
    private final String warehouseId;
    private final List<Item> items;
    private final Instant createdAt;

    private InventoryReservation(
                String reservationId,
                String orderId,
                String warehouseId,
                List<Item> items,
                Instant createdAt) {
        this.reservationId = requireNonBlank(reservationId, "reservationId");
        this.orderId = requireNonBlank(orderId, "orderId");
        this.warehouseId = requireNonBlank(warehouseId, "warehouseId");

        if (items.isEmpty()) throw new IllegalArgumentException("items cant be empty");
        this.items = Objects.requireNonNull(items, "items");

        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    public static InventoryReservation createInventoryReservation(
            String reservationId,
            String orderId,
            String warehouseId,
            List<Item> items) {
        return new InventoryReservation(
            reservationId,
            orderId,
            warehouseId,
            items,
            Instant.now()
        );
    }
    
  public record Item(String sku, int quantity){}

}
