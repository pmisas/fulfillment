package com.fulfillment.orderservice.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fulfillment.orderservice.domain.exception.InvalidStatusTransitionException;

class OrderTest {

    @Test
    void createOrder_shouldCreateOrderWithReceivedStatus() {
        Order order = Order.createOrder(
            "order-1",
            "operator-1",
            4.7110,
            -74.0721,
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        assertEquals("order-1", order.getOrderId());
        assertEquals(Status.RECEIVED, order.getStatus());
        assertEquals(4.7110, order.getLat());
        assertEquals(-74.0721, order.getLng());
        assertEquals(1, order.getItems().size());
    }

    @Test
    void createOrder_shouldThrowWhenOrderIdIsBlank() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> Order.createOrder(
                " ",
                "operator-1",
                4.7110,
                -74.0721,
                List.of(OrderItem.createOrderItem("SKU-1", 2))
            )
        );

        assertEquals("orderId must be not blank", ex.getMessage());
    }

    @Test
    void createOrder_shouldThrowWhenItemsAreEmpty() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> Order.createOrder("order-1", "operator-1", 4.7110, -74.0721, List.of())
        );

        assertTrue(ex.getMessage().toLowerCase().contains("items"));
    }

    @Test
    void withStatus_shouldAllowValidTransition() {
        Order order = Order.createOrder(
            "order-1",
            "operator-1",
            4.7110,
            -74.0721,
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        Order validated = order.withStatus(Status.VALIDATED);

        assertEquals(Status.VALIDATED, validated.getStatus());
        assertEquals(order.getOrderId(), validated.getOrderId());
    }

    @Test
    void withStatus_shouldThrowWhenTransitionIsInvalid() {
        Order order = Order.createOrder(
            "order-1",
            "operator-1",
            4.7110,
            -74.0721,
            List.of(OrderItem.createOrderItem("SKU-1", 2))
        );

        assertThrows(
            InvalidStatusTransitionException.class,
            () -> order.withStatus(Status.SHIPPED)
        );
    }
}
