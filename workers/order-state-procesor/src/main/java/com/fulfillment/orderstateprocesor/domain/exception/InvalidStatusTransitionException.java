package com.fulfillment.orderstateprocesor.domain.exception;

import com.fulfillment.orderstateprocesor.domain.model.Status;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(Status from, Status to) {
        super("Invalid transition: " + from + " -> " + to);
    }
}