/*package com.fulfillment.orderservice.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class OrderStateHistoryTest {
    
    @Test
    void createOrderStateHistory_whenValid__shouldCreate() {

        String orderId = "id01";

        OrderStateHistory history = OrderStateHistory.createOrderStateHistory(orderId);

        assertNotNull(history.getHistoryId());
        assertEquals(orderId, history.getOrderId());
        assertNull(history.getFromStatus());
        assertEquals(Status.CREATED, history.getToStatus());
        assertNotNull(history.getchangedAt());
    }

    @Test
    void reateOrderStateHistory_whenOrderIsBlank__shouldThrowIllegalArgumentException() {

        String orderId = " ";

        assertThrows(IllegalArgumentException.class, 
                        () -> OrderStateHistory.createOrderStateHistory(orderId)
        );
    }

    @Test
    void transitionOrderStateHistory_shouldCreate() {

        String orderId = "id01";
        Status fromStatus = Status.CONFIRMED;
        Status toStatus = Status.PACKED;

        OrderStateHistory history = OrderStateHistory
                        .transitionOrderStateHistory(
                            orderId, 
                            fromStatus, 
                            toStatus
        );

        assertNotNull(history.getHistoryId());
        assertEquals(orderId, history.getOrderId());
        assertEquals(fromStatus, history.getFromStatus());
        assertEquals(toStatus, history.getToStatus());
        assertNotNull(history.getchangedAt());
    }

    @Test
    void transitionOrderStateHistory_whenFromIsNull_shoulThrowNullPointerException() {

        String orderId = "id01";
        Status fromStatus = null;
        Status toStatus = Status.PACKED;

        assertThrows(NullPointerException.class,() -> 
                        OrderStateHistory.transitionOrderStateHistory(
                            orderId, 
                            fromStatus, 
                            toStatus
                        ));
    }

    @Test
    void transitionOrderStateHistory_whenToIsNull_shoulThrowNullPointerException() {

        String orderId = "id01";
        Status fromStatus = Status.PACKED;
        Status toStatus = null;

        assertThrows(NullPointerException.class,() -> 
                        OrderStateHistory.transitionOrderStateHistory(
                            orderId, 
                            fromStatus, 
                            toStatus
                        ));
    }

    @Test
    void transitionOrderStateHistory_whenToEqualsFrom_shouldThrowIllegalArgumentEception() {
        
        String orderId = "id01";
        Status fromStatus = Status.PACKED;
        Status toStatus = Status.PACKED;

        assertThrows(IllegalArgumentException.class,() -> 
                        OrderStateHistory.transitionOrderStateHistory(
                            orderId, 
                            fromStatus, 
                            toStatus
                        ));
    }
}
*/