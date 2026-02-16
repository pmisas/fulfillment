package com.fulfillment.orderservice.domain.exception;

public class IdempotencyInconsistentStateException extends RuntimeException {
    public IdempotencyInconsistentStateException(String idempotencyKey, String orderId) {
        super("Idempotency key '" + idempotencyKey + "' points to missing orderId '" + orderId + "'");
    }
}
