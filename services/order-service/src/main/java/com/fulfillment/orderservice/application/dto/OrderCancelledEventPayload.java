package com.fulfillment.orderservice.application.dto;

public record OrderCancelledEventPayload(
    String orderId,
    String reason
) {}
