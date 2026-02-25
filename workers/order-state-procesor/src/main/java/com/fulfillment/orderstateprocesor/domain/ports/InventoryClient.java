package com.fulfillment.orderstateprocesor.domain.ports;

import java.util.List;

public interface InventoryClient {

    void reserve(String warehouseId, String sku, int amount);
    void release(String warehouseId, String sku, int amount);
    AvailabilityResult checkAvailability(String warehouseId, List<SkuQuantity> items);

    record SkuQuantity(String sku, int quantity) {}

    record AvailabilityResult(
        boolean canFulfillAll,
        List<ItemAvailability> items
    ) {}

    record ItemAvailability(
        String sku,
        int required,
        int available,
        boolean canFulfill
    ) {}
}
