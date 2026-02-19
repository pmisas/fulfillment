package com.fulfillment.orderservice.domain.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class StatusTest {
    
    @Test
    void canTransitionTo_createdToConfirmed_shouldAllow() {
        
        boolean result = Status.CREATED.canTransitionTo(Status.CONFIRMED);

        assertTrue(result);
    }

    @Test
    void canTransitionTo_confirmedToPacked_shouldAllow() {

        boolean result = Status.CONFIRMED.canTransitionTo(Status.PACKED);

        assertTrue(result);
    }

    @Test
    void canTransitionTo_packedToShipped_shouldAllow() {
        
        boolean result = Status.PACKED.canTransitionTo(Status.SHIPPED);

        assertTrue(result);
    }

    @Test
    void canTransitionTo_createdToCanceled_shouldAllow() {

        boolean result = Status.CREATED.canTransitionTo(Status.CANCELED);

        assertTrue(result);
    }

    @Test
    void canTransitionTo_confirmedToCanceled_shouldAllow() {

        boolean result = Status.CONFIRMED.canTransitionTo(Status.CANCELED);

        assertTrue(result);
    }

    @Test
    void canTransitionTo_packedToCanceled_shouldNotAllow() {

        boolean result = Status.PACKED.canTransitionTo(Status.CANCELED);

        assertFalse(result);
    }

    @Test
    void canTransitionTo_shippedIsTerminalState() {

        assertFalse(Status.SHIPPED.canTransitionTo(Status.CREATED));
        assertFalse(Status.SHIPPED.canTransitionTo(Status.CONFIRMED));
        assertFalse(Status.SHIPPED.canTransitionTo(Status.PACKED));
        assertFalse(Status.SHIPPED.canTransitionTo(Status.CANCELED));
    }

    @Test
    void canTransitionTo_canceledIsTerminalState() {

        assertFalse(Status.CANCELED.canTransitionTo(Status.CREATED));
        assertFalse(Status.CANCELED.canTransitionTo(Status.CONFIRMED));
        assertFalse(Status.CANCELED.canTransitionTo(Status.PACKED));
        assertFalse(Status.CANCELED.canTransitionTo(Status.SHIPPED));
    }

}
