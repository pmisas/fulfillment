package com.fulfillment.orderstateprocesor.domain.ports;

import java.util.List;

public interface InventoryClient {

    ReserveResult reserveAll(String reservationId, String orderId, String warehouseId, List<SkuQuantity> items);
    void releaseReservation(String reservationId);
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

    enum ReserveResult { RESERVED, ALREADY_RESERVED, INSUFFICIENT_STOCK }
}
