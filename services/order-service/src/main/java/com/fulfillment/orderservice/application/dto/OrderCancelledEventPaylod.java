package com.fulfillment.orderservice.application.dto;

public record OrderCancelledEventPayload(
    String order_id,
    String reason
) {}
