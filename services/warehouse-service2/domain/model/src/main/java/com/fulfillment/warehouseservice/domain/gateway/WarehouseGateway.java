package com.fulfillment.warehouseservice.domain.gateway;

import com.fulfillment.warehouseservice.domain.model.Warehouse;
import reactor.core.publisher.Mono;

public interface WarehouseGateway {

    Mono<Warehouse> save(Warehouse warehouse);

    Mono<Warehouse> findById(String warehouseId);

    Mono<Void> deleteById(String warehouseId);
}
