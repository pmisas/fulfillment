package com.fulfillment.inventoryservice.domain.model;

import java.time.Instant;
import java.util.Objects;

import com.fulfillment.inventoryservice.domain.exception.InsufficientAvailableStockException;
import com.fulfillment.inventoryservice.domain.exception.InsufficientReservedStockException;

import lombok.Getter;

import static com.fulfillment.inventoryservice.domain.shared.DomainValidations.requireNonBlank;

@Getter
public class InventoryItem {
    
    private final String warehouseId;
    private final String sku;
    private final int quantity;
    private final int reserved;
    private final Instant updatedAt;

    private InventoryItem(
                String warehouseId,
                String sku,
                int quantity,
                int reserved,
                Instant updatedAt) {
        this.warehouseId = requireNonBlank(warehouseId, "warehouseId").trim();
        this.sku = requireNonBlank(sku, "sku").trim();

        if (quantity < 0) 
            throw new IllegalArgumentException("quantity must be >= 0");
        if (reserved < 0)  
            throw new IllegalArgumentException("reserved must be >= 0");
        if (reserved > quantity) 
            throw new IllegalArgumentException("reserved cant be > quantity");

        this.quantity = quantity;
        this.reserved = reserved;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static InventoryItem createInventoryItem(
                        String warehouseId,
                        String sku,
                        int quantity) {
        return new InventoryItem(
            warehouseId,
            sku,
            quantity,
            0,
            Instant.now()
        );
    }

    public static InventoryItem restore(
                String warehouseId,
                String sku,
                int quantity,
                int reserved,
                Instant updateAt) {
        return new InventoryItem(warehouseId, sku, quantity, reserved, updateAt);
    }

    public InventoryItem restock(int amount) {
        if (amount <= 0) 
            throw new IllegalArgumentException("amount must be > 0");

        return new InventoryItem(
            warehouseId, 
            sku, 
            amount + quantity, 
            reserved, 
            Instant.now()
        );
    }

    public InventoryItem consume(int amount) {
        if (amount <= 0) 
            throw new IllegalArgumentException("amount must be > 0");
        if (amount > reserved)
            throw new InsufficientReservedStockException(amount, warehouseId, sku, reserved);
        if (amount > quantity)
            throw new  IllegalArgumentException("amount cannot exceed quantity");

        return new InventoryItem(
            warehouseId, 
            sku, 
            quantity - amount, 
            reserved - amount, 
            Instant.now()
        );
    }

    public InventoryItem reserve(int amount) {
        if (amount <= 0) 
            throw new IllegalArgumentException("amount must be > 0");
        if (amount > available()) 
            throw new InsufficientAvailableStockException(amount, warehouseId, sku, available());

        return new InventoryItem(
            warehouseId, 
            sku, 
            quantity, 
            amount + reserved, 
            Instant.now()
        );
    }

    public InventoryItem release(int amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("amount must be > 0");
        if (reserved < amount) 
            throw new InsufficientReservedStockException(amount, warehouseId, sku, reserved);
        
        return new InventoryItem(
            warehouseId, 
            sku, 
            quantity, 
            reserved - amount, 
            Instant.now()
        );
    }

    public int available() {
        return quantity - reserved;
    }

} 
