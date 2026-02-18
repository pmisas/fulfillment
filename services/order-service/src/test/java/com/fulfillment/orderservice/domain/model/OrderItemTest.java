package com.fulfillment.orderservice.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OrderItemTest {
    
    @Test
    void createOrderItem_whenValid_shouldCreate() {

        String sku = "SKU-APPLE-01";
        int quantity = 2;

        OrderItem item = OrderItem.createOrderItem(sku, quantity);

        assertEquals("SKU-APPLE-01", item.getSku());
        assertEquals(2, item.getQuantity());
    }

    @Test
    void createOrderItem_whenQuantityIsZero_shouldThrowIllegalArgumentException() {
        String sku = "SKU-APPLE-01";
        int quantity = 0;

        assertThrows(IllegalArgumentException.class,
            () ->  OrderItem.createOrderItem(sku, quantity)
        );
        
    }

    @Test
    void createOrderItem_whenSkuIsBlank_shouldThrowIllegalArgumentException() {
        String sku = " ";
        int quantity = 1;

        assertThrows(IllegalArgumentException.class,
            () ->  OrderItem.createOrderItem(sku, quantity)
        );
    }

}
