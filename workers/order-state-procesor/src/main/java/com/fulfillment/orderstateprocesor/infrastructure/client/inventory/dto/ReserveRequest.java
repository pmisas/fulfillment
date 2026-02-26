package com.fulfillment.orderstateprocesor.infrastructure.client.inventory.dto;

import java.util.List;

public record ReserveRequest(
    String reservationId,
    String orderId,
    List<SkuQuantityDto> items
) {
    public record SkuQuantityDto(String sku, int quantity) {}
}
