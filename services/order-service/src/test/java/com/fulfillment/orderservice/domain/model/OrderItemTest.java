package com.fulfillment.orderservice.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OrderItemTest {

    @Test
    void createOrderItem_shouldCreateItemWhenDataIsValid() {
        OrderItem item = OrderItem.createOrderItem("SKU-1", 2);

        assertEquals("SKU-1", item.getSku());
        assertEquals(2, item.getQuantity());
    }

    @Test
    void createOrderItem_shouldThrowWhenSkuIsNull() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> OrderItem.createOrderItem(null, 2)
        );

        assertEquals("sku must be not blank", ex.getMessage());
    }

    @Test
    void createOrderItem_shouldThrowWhenSkuIsBlank() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> OrderItem.createOrderItem("   ", 2)
        );

        assertEquals("sku must be not blank", ex.getMessage());
    }

    @Test
    void createOrderItem_shouldThrowWhenQuantityIsZero() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> OrderItem.createOrderItem("SKU-1", 0)
        );

        assertEquals("quantity must be > 0", ex.getMessage());
    }

    @Test
    void createOrderItem_shouldThrowWhenQuantityIsNegative() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> OrderItem.createOrderItem("SKU-1", -1)
        );

        assertEquals("quantity must be > 0", ex.getMessage());
    }
}
