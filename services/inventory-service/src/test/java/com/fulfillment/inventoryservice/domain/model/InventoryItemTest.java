package com.fulfillment.inventoryservice.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.fulfillment.inventoryservice.domain.exception.InsufficientAvailableStockException;
import com.fulfillment.inventoryservice.domain.exception.InsufficientReservedStockException;

class InventoryItemTest {

    @Test
    void createInventoryItem_shouldCreateWithReservedZero() {
        InventoryItem item = InventoryItem.createInventoryItem("wh-1", "SKU-1", 10);

        assertEquals("wh-1", item.getWarehouseId());
        assertEquals("SKU-1", item.getSku());
        assertEquals(10, item.getQuantity());
        assertEquals(0, item.getReserved());
        assertEquals(10, item.available());
        assertNotNull(item.getUpdatedAt());
    }

    @Test
    void restore_shouldThrowWhenQuantityIsNegative() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> InventoryItem.restore("wh-1", "SKU-1", -1, 0, Instant.now())
        );

        assertEquals("quantity must be >= 0", ex.getMessage());
    }

    @Test
    void restore_shouldThrowWhenReservedIsNegative() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> InventoryItem.restore("wh-1", "SKU-1", 10, -1, Instant.now())
        );

        assertEquals("reserved must be >= 0", ex.getMessage());
    }

    @Test
    void restore_shouldThrowWhenReservedIsGreaterThanQuantity() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> InventoryItem.restore("wh-1", "SKU-1", 5, 6, Instant.now())
        );

        assertEquals("reserved cant be > quantity", ex.getMessage());
    }

    @Test
    void restock_shouldIncreaseQuantity() {
        InventoryItem item = InventoryItem.restore("wh-1", "SKU-1", 10, 2, Instant.now());

        InventoryItem updated = item.restock(5);

        assertEquals(15, updated.getQuantity());
        assertEquals(2, updated.getReserved());
        assertEquals(13, updated.available());
    }

    @Test
    void restock_shouldThrowWhenAmountIsNotPositive() {
        InventoryItem item = InventoryItem.restore("wh-1", "SKU-1", 10, 2, Instant.now());

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> item.restock(0)
        );

        assertEquals("amount must be > 0", ex.getMessage());
    }

    @Test
    void reserve_shouldIncreaseReservedWhenEnoughAvailable() {
        InventoryItem item = InventoryItem.restore("wh-1", "SKU-1", 10, 2, Instant.now());

        InventoryItem updated = item.reserve(3);

        assertEquals(10, updated.getQuantity());
        assertEquals(5, updated.getReserved());
        assertEquals(5, updated.available());
    }

    @Test
    void reserve_shouldThrowWhenAmountExceedsAvailable() {
        InventoryItem item = InventoryItem.restore("wh-1", "SKU-1", 10, 8, Instant.now());

        assertThrows(
            InsufficientAvailableStockException.class,
            () -> item.reserve(3)
        );
    }

    @Test
    void release_shouldDecreaseReserved() {
        InventoryItem item = InventoryItem.restore("wh-1", "SKU-1", 10, 5, Instant.now());

        InventoryItem updated = item.release(3);

        assertEquals(10, updated.getQuantity());
        assertEquals(2, updated.getReserved());
        assertEquals(8, updated.available());
    }

    @Test
    void release_shouldThrowWhenAmountExceedsReserved() {
        InventoryItem item = InventoryItem.restore("wh-1", "SKU-1", 10, 2, Instant.now());

        assertThrows(
            InsufficientReservedStockException.class,
            () -> item.release(3)
        );
    }

    @Test
    void consume_shouldDecreaseQuantityAndReserved() {
        InventoryItem item = InventoryItem.restore("wh-1", "SKU-1", 10, 4, Instant.now());

        InventoryItem updated = item.consume(3);

        assertEquals(7, updated.getQuantity());
        assertEquals(1, updated.getReserved());
        assertEquals(6, updated.available());
    }

    @Test
    void consume_shouldThrowWhenAmountExceedsReserved() {
        InventoryItem item = InventoryItem.restore("wh-1", "SKU-1", 10, 2, Instant.now());

        assertThrows(
            InsufficientReservedStockException.class,
            () -> item.consume(3)
        );
    }
}
