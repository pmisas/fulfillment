package com.fulfillment.shippingservice.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ShipmentItemTest {

    @Test
    void createShipmentItem_shouldCreateWhenDataIsValid() {
        ShipmentItem item = ShipmentItem.createShipmentItem("SKU-1", 2);

        assertEquals("SKU-1", item.getSku());
        assertEquals(2, item.getQuantity());
    }

    @Test
    void createShipmentItem_shouldThrowWhenSkuIsNull() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ShipmentItem.createShipmentItem(null, 2)
        );

        assertEquals("sku must be not blank", ex.getMessage());
    }

    @Test
    void createShipmentItem_shouldThrowWhenSkuIsBlank() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ShipmentItem.createShipmentItem("   ", 2)
        );

        assertEquals("sku must be not blank", ex.getMessage());
    }

    @Test
    void createShipmentItem_shouldThrowWhenQuantityIsZero() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ShipmentItem.createShipmentItem("SKU-1", 0)
        );

        assertEquals("quantity must be greater than 0", ex.getMessage());
    }

    @Test
    void createShipmentItem_shouldThrowWhenQuantityIsNegative() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> ShipmentItem.createShipmentItem("SKU-1", -1)
        );

        assertEquals("quantity must be greater than 0", ex.getMessage());
    }
}
