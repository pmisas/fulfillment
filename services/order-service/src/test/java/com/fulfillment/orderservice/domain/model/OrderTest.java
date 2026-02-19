package com.fulfillment.orderservice.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fulfillment.orderservice.domain.exception.InvalidStatusTransitionException;

public class OrderTest {
    
    @Test
    void createOrder_whenValid_souldCreate() {

        String warehouseId = "wh-123";
        String customerId = "paula";
        List<OrderItem> items = List.of(
                OrderItem.createOrderItem("SKU-BANANA-12", 1),
                OrderItem.createOrderItem("SKU-FRESA-12", 4)
        );

        Order order = Order.createOrder(warehouseId, customerId, items);
    
        assertNotNull(order.getOrderId());
        assertEquals("wh-123", order.getWerehouseId());
        assertEquals("paula", order.getCustomerId());
        assertEquals(Status.CREATED, order.getStatus());
        assertNotNull(order.getCreatedAt());
        assertNotNull(order.getUpdatedAt());
        assertEquals(2, order.getItems().size());
    }

    @Test
    void createOrder_whenWherehouseIsBlank_shouldThrowIllegalArgumentException() {

        String warehouseId = " ";
        String customerId = "paula";
        List<OrderItem> items = List.of(
                OrderItem.createOrderItem("SKU-BANANA-12", 1),
                OrderItem.createOrderItem("SKU-FRESA-12", 4)
        );

        assertThrows(IllegalArgumentException.class, 
                () -> Order.createOrder(warehouseId, customerId, items)
        );
    }

    @Test
    void createOrder_whenCustomerIsBlank_shouldThrowIllegalArgumentException() {

        String warehouseId = "wh-123";
        String customerId = " ";
        List<OrderItem> items = List.of(
                OrderItem.createOrderItem("SKU-BANANA-12", 1),
                OrderItem.createOrderItem("SKU-FRESA-12", 4)
        );

        assertThrows(IllegalArgumentException.class, 
                () -> Order.createOrder(warehouseId, customerId, items)
        );
    }

    @Test
    void createOrder_whenItemsEmpty_shouldThrowIllegalArgumentException() {

        String warehouseId = "wh-123";
        String customerId = " ";
        List<OrderItem> items = List.of();

        assertThrows(IllegalArgumentException.class, 
                () -> Order.createOrder(warehouseId, customerId, items)
        );
    }

    @Test
    void withStatus_shouldAllowCreatedToConfirmed() {

        Order order = givenCreatedOrder();

        Order updated = order.withStatus(Status.CONFIRMED);

        assertEquals(Status.CONFIRMED, updated.getStatus());
        assertEquals(updated.getOrderId(), order.getOrderId());
    }

    @Test
    void withStatus_shouldAllowConfirmedToPacked() {
        
        Order order = givenCreatedOrder().withStatus(Status.CONFIRMED);

        Order updated = order.withStatus(Status.PACKED);

        assertEquals(Status.PACKED, updated.getStatus());
        assertEquals(updated.getOrderId(), order.getOrderId());
    }

    @Test
    void withStatus_shouldAllowPackedToShipped() {
        
        Order order = givenCreatedOrder()
                        .withStatus(Status.CONFIRMED)
                        .withStatus(Status.PACKED);

        Order updated = order.withStatus(Status.SHIPPED);

        assertEquals(Status.SHIPPED, updated.getStatus());
        assertEquals(updated.getOrderId(), order.getOrderId());
    }

    @Test
    void withStatus_shouldAllowCreatedToCanceled() {
        
        Order order = givenCreatedOrder();

        Order updated = order.withStatus(Status.CANCELED);

        assertEquals(Status.CANCELED, updated.getStatus());
        assertEquals(updated.getOrderId(), order.getOrderId());
    }

    @Test
    void withStatus_shouldAllowConfirmedToCanceled() {
        
        Order order = givenCreatedOrder().withStatus(Status.CONFIRMED);

        Order updated = order.withStatus(Status.CANCELED);

        assertEquals(Status.CANCELED, updated.getStatus());
        assertEquals(updated.getOrderId(), order.getOrderId());
    }

    @Test
    void withStatus_shouldRejectedCreatedToShipped() {
        
        Order order = givenCreatedOrder();

        assertThrows(InvalidStatusTransitionException.class, 
                    () -> order.withStatus(Status.SHIPPED));
    }

    @Test
    void withStatus_shouldRejectedPackedToCanceled() {
        
        Order order = givenCreatedOrder()
                        .withStatus(Status.CONFIRMED)
                        .withStatus(Status.PACKED);

        assertThrows(InvalidStatusTransitionException.class, 
                    () -> order.withStatus(Status.CANCELED));
    }

    private Order givenCreatedOrder() {
        return Order.createOrder(
                "wh-1", 
                "cus-1",   
                List.of(OrderItem.createOrderItem("SKU-1", 1))
        );
    }
}
