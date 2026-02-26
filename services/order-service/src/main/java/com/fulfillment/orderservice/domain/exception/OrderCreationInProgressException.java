package com.fulfillment.orderservice.domain.exception;

public class OrderCreationInProgressException extends RuntimeException {
    public OrderCreationInProgressException(String idempotencyKey) {
        super("An order creation is already in progress for idempotency key: " + idempotencyKey);
    }
}
