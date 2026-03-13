package com.fulfillment.orderservice.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class OrderStateHistoryTest {

    @Test
    void createOrderStateHistory_shouldCreateInitialHistory() {
        OrderStateHistory history = OrderStateHistory.createOrderStateHistory("hist-1", "order-1");

        assertEquals("hist-1", history.getHistoryId());
        assertEquals("order-1", history.getOrderId());
        assertNull(history.getFromStatus());
        assertEquals(Status.RECEIVED, history.getToStatus());
        assertNotNull(history.getChangedAt());
    }

    @Test
    void createOrderStateHistory_shouldThrowWhenHistoryIdIsBlank() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> OrderStateHistory.createOrderStateHistory(" ", "order-1")
        );

        assertEquals("historyId must be not blank", ex.getMessage());
    }

    @Test
    void createOrderStateHistory_shouldThrowWhenOrderIdIsBlank() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> OrderStateHistory.createOrderStateHistory("hist-1", " ")
        );

        assertEquals("orderId must be not blank", ex.getMessage());
    }

    @Test
    void transitionOrderStateHistory_shouldCreateTransitionHistory() {
        OrderStateHistory history = OrderStateHistory.transitionOrderStateHistory(
            "order-1",
            Status.RECEIVED,
            Status.VALIDATED
        );

        assertNotNull(history.getHistoryId());
        assertEquals("order-1", history.getOrderId());
        assertEquals(Status.RECEIVED, history.getFromStatus());
        assertEquals(Status.VALIDATED, history.getToStatus());
        assertNotNull(history.getChangedAt());
    }

    @Test
    void transitionOrderStateHistory_shouldThrowWhenFromStatusIsNull() {
        NullPointerException ex = assertThrows(
            NullPointerException.class,
            () -> OrderStateHistory.transitionOrderStateHistory("order-1", null, Status.VALIDATED)
        );

        assertEquals("fromStatus", ex.getMessage());
    }

    @Test
    void transitionOrderStateHistory_shouldThrowWhenStatusesAreEqual() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> OrderStateHistory.transitionOrderStateHistory("order-1", Status.RECEIVED, Status.RECEIVED)
        );

        assertEquals("fromStatus and toStatus must be different", ex.getMessage());
    }

    @Test
    void restore_shouldRestoreHistoryCorrectly() {
        Instant now = Instant.now();

        OrderStateHistory history = OrderStateHistory.restore(
            "hist-1",
            "order-1",
            Status.RECEIVED,
            Status.VALIDATED,
            now
        );

        assertEquals("hist-1", history.getHistoryId());
        assertEquals("order-1", history.getOrderId());
        assertEquals(Status.RECEIVED, history.getFromStatus());
        assertEquals(Status.VALIDATED, history.getToStatus());
        assertEquals(now, history.getChangedAt());
    }
}
