package com.fulfillment.shippingservice.domain.exception;

import com.fulfillment.shippingservice.domain.model.ShipmentStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(ShipmentStatus from, ShipmentStatus to) {
        super("Invalid status transition from " + from + " to " + to);
    }
}
