package com.fulfillment.inventoryservice.application.dto;

import java.util.List;

public record AvailabilityResult(
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
