package com.fulfillment.orderstateprocesor.domain.ports;

import java.util.List;

import reactor.core.publisher.Mono;

public interface InventoryClient {

    Mono<ReserveResult> reserveAll(String reservationId, String orderId, String warehouseId, List<SkuQuantity> items);
    Mono<Void> releaseReservation(String reservationId);
    Mono<ConsumeResult> consumeReservation(String reservationId);
    Mono<AvailabilityResult> checkAvailability(String warehouseId, List<SkuQuantity> items);

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
    enum ConsumeResult { CONSUMED, RESERVATION_NOT_FOUND }
}
