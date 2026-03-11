package com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto;

public record OrderCancelledEvent(
    String orderId,
    String reason
) {}
