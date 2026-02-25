package com.fulfillment.orderstateprocesor.infrastructure.client.inventory.dto;

import java.util.List;

public record AvailabilityRequest(List<SkuQuantityDto> items) {
    public record SkuQuantityDto(String sku, int quantity) {}
}
