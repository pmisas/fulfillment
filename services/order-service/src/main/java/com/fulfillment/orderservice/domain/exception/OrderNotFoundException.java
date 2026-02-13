package com.fulfillment.orderservice.domain.exception;

public class OrderNotFoundException extends RuntimeException{
    public OrderNotFoundException(String orderId) {
        super("Order not found exception" + orderId);
    }
    
}
