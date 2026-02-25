package com.fulfillment.inventoryservice.application.dto;

import java.util.List;

public record AvailabilityQuery(
    String warehouseId,
    List<SkuQuantity> items
) {
    public record SkuQuantity(String sku, int quantity) {}
}
