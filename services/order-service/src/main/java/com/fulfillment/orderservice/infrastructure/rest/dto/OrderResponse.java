package com.fulfillment.orderservice.infrastructure.rest.dto;

import java.util.List;

public record OrderResponse(
    String orderId,
    String customerId,
    String status,
    List<Item> items
) {
    public record Item(
        String sku,
        int quantity
    ) {}
}
