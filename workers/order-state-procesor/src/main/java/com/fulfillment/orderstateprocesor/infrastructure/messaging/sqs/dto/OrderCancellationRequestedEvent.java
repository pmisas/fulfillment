package com.fulfillment.orderstateprocesor.infrastructure.messaging.sqs.dto;

public record OrderCancellationRequestedEvent(
    String orderId,
    String reason
) {}
