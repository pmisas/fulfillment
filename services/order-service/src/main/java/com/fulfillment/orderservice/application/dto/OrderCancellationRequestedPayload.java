package com.fulfillment.orderservice.application.dto;

public record OrderCancellationRequestedPayload(
    String orderId,
    String reason
) {}
