package com.fulfillment.orderservice.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StatusTest {

    @Test
    void received_shouldAllowValidatedRejectedAndCanceled() {
        assertTrue(Status.RECEIVED.canTransitionTo(Status.VALIDATED));
        assertTrue(Status.RECEIVED.canTransitionTo(Status.REJECTED));
        assertTrue(Status.RECEIVED.canTransitionTo(Status.CANCELED));
    }

    @Test
    void received_shouldNotAllowPickedPackedOrShipped() {
        assertFalse(Status.RECEIVED.canTransitionTo(Status.PICKED));
        assertFalse(Status.RECEIVED.canTransitionTo(Status.PACKED));
        assertFalse(Status.RECEIVED.canTransitionTo(Status.SHIPPED));
    }

    @Test
    void validated_shouldAllowPickedAndCanceled() {
        assertTrue(Status.VALIDATED.canTransitionTo(Status.PICKED));
        assertTrue(Status.VALIDATED.canTransitionTo(Status.CANCELED));
    }

    @Test
    void picked_shouldAllowPackedAndCanceled() {
        assertTrue(Status.PICKED.canTransitionTo(Status.PACKED));
        assertTrue(Status.PICKED.canTransitionTo(Status.CANCELED));
    }

    @Test
    void packed_shouldAllowOnlyShipped() {
        assertTrue(Status.PACKED.canTransitionTo(Status.SHIPPED));
        assertFalse(Status.PACKED.canTransitionTo(Status.CANCELED));
        assertFalse(Status.PACKED.canTransitionTo(Status.REJECTED));
    }

    @Test
    void terminalStates_shouldNotAllowFurtherTransitions() {
        for (Status status : new Status[]{Status.DELIVERED, Status.REJECTED, Status.CANCELED}) {
            for (Status target : Status.values()) {
                assertFalse(status.canTransitionTo(target));
            }
        }
    }
}
