package com.fulfillment.orderstateprocesor.infrastructure.client.inventory.dto;

import java.util.List;

public record AvailabilityResponse(
    boolean canFulfillAll,
    List<ItemAvailabilityDto> items
) {
    public record ItemAvailabilityDto(
        String sku,
        int required,
        int available,
        boolean canFulfill
    ) {}
}
