package com.fulfillment.orderservice.domain.exception;

public class OrderNotOwnedException extends RuntimeException {

    public OrderNotOwnedException(String orderId) {
        super("Access denied: you don't own order " + orderId);
    }
}
