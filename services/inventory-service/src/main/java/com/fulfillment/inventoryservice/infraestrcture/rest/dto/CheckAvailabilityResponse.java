package com.fulfillment.inventoryservice.infraestrcture.rest.dto;

import java.util.List;

public record CheckAvailabilityResponse(
    boolean canFulfillAll,
    List<ItemAvailability> items
) {
    public record ItemAvailability(
        String sku,
        int required,
        int available,
        boolean canFulfill
    ) {}
}
