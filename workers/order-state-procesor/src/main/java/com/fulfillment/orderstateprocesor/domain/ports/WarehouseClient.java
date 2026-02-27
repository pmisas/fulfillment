package com.fulfillment.orderstateprocesor.domain.ports;

import java.util.List;

import reactor.core.publisher.Mono;

public interface WarehouseClient {

    Mono<Boolean> existsById(String warehouseId);
    Mono<List<WarehouseSummary>> listWarehouses();

    record WarehouseSummary(String warehouseId, double lat, double lng) {}
}
