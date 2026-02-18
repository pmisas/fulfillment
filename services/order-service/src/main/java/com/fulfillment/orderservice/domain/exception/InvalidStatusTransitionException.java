package com.fulfillment.orderservice.domain.exception;

import com.fulfillment.orderservice.domain.model.Status;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(Status actual, Status next) {
        super("Invalid transition from " + actual + " to " + next);
    }
}
