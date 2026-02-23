package com.fulfillment.orderservice.infrastructure.rest.dto;

public record OrderResponse(
    String orderId,
    String status
) {
}
